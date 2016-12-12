package io.sweers.catchup.rx.boundlifecycle;

import com.jakewharton.rxrelay2.PublishRelay;
import com.jakewharton.rxrelay2.Relay;
import io.reactivex.Completable;
import io.reactivex.CompletableObserver;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.MaybeObserver;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.functions.Cancellable;
import io.reactivex.functions.Function;
import io.reactivex.processors.FlowableProcessor;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;
import io.sweers.catchup.rx.boundlifecycle.observers.BoundObservers;
import io.sweers.catchup.rx.boundlifecycle.observers.Disposables;
import io.sweers.catchup.ui.base.LifecycleEndedException;
import io.sweers.catchup.ui.base.LifecycleNotStartedException;
import io.sweers.testutils.RecordingObserver2;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nonnull;
import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class BoundObserverTest {

  private static final Function<Integer, Integer> CORRESPONDING_EVENTS = lastEvent -> {
    switch (lastEvent) {
      case 0:
        return 3;
      case 1:
        return 2;
      default:
        throw new LifecycleEndedException("Out of lifecycle");
    }
  };

  @Test
  public void testBoundObserver() {
    AtomicInteger valHolder = new AtomicInteger(0);
    PublishSubject<Integer> source = PublishSubject.create();
    PublishSubject<Integer> lifecycle = PublishSubject.create();
    source.subscribe(BoundObservers.<Integer>forObservable(lifecycle).onNext(valHolder::set)
        .create());

    assertThat(source.hasObservers()).isTrue();
    assertThat(lifecycle.hasObservers()).isTrue();

    source.onNext(1);
    assertThat(valHolder.get()).isEqualTo(1);

    lifecycle.onNext(2);
    source.onNext(2);
    assertThat(valHolder.get()).isEqualTo(1);
    assertThat(source.hasObservers()).isFalse();
    assertThat(lifecycle.hasObservers()).isFalse();
  }

  @Test
  public void testBoundObserver_withProvider() {
    RecordingObserver2<Integer> o = new RecordingObserver2<>();
    PublishSubject<Integer> source = PublishSubject.create();
    BehaviorSubject<Integer> lifecycle = BehaviorSubject.createDefault(0);
    LifecycleProvider<Integer> provider = makeProvider(lifecycle);
    source.subscribe(BoundObservers.<Integer>forObservable(provider).around(o));

    assertThat(source.hasObservers()).isTrue();
    assertThat(lifecycle.hasObservers()).isTrue();

    source.onNext(1);
    assertThat(o.takeNext()).isEqualTo(1);

    lifecycle.onNext(1);
    source.onNext(2);

    assertThat(source.hasObservers()).isTrue();
    assertThat(lifecycle.hasObservers()).isTrue();
    assertThat(o.takeNext()).isEqualTo(2);

    lifecycle.onNext(3);
    source.onNext(3);

    o.assertNoMoreEvents();
    assertThat(source.hasObservers()).isFalse();
    assertThat(lifecycle.hasObservers()).isFalse();
  }

  @Test
  public void testBoundObserver_withProvider_withoutStartingLifecycle_shouldFail() {
    BehaviorSubject<Integer> lifecycle = BehaviorSubject.create();
    RecordingObserver2<Integer> o = new RecordingObserver2<>();
    LifecycleProvider<Integer> provider = makeProvider(lifecycle);
    Observable.just(1)
        .subscribe(Disposables.forObservable(provider)
            .around(o));

    assertThat(o.takeError()).isInstanceOf(LifecycleNotStartedException.class);
  }

  @Test
  public void testBoundObserver_withProvider_afterLifecycle_shouldFail() {
    BehaviorSubject<Integer> lifecycle = BehaviorSubject.createDefault(0);
    lifecycle.onNext(1);
    lifecycle.onNext(2);
    lifecycle.onNext(3);
    RecordingObserver2<Integer> o = new RecordingObserver2<>();
    LifecycleProvider<Integer> provider = makeProvider(lifecycle);
    Observable.just(1)
        .subscribe(Disposables.forObservable(provider)
            .around(o));

    assertThat(o.takeError()).isInstanceOf(LifecycleEndedException.class);
  }

  @Test
  public void verifyCancellation() throws Exception {
    Cancellable cancellable = mock(Cancellable.class);
    //noinspection unchecked because Java
    final ObservableEmitter<Integer>[] emitter = new ObservableEmitter[1];
    Observable<Integer> source = Observable.create(e -> {
      e.setCancellable(cancellable);
      emitter[0] = e;
    });
    PublishSubject<Integer> lifecycle = PublishSubject.create();
    source.subscribe(BoundObservers.<Integer>forObservable(lifecycle).create());

    verify(cancellable, never()).cancel();
    assertThat(lifecycle.hasObservers()).isTrue();

    emitter[0].onNext(1);

    lifecycle.onNext(2);
    emitter[0].onNext(2);
    verify(cancellable).cancel();
    assertThat(lifecycle.hasObservers()).isFalse();
  }

  @Test
  public void alternateCreator() {
    PublishSubject<Integer> source = PublishSubject.create();
    PublishSubject<Integer> lifecycle = PublishSubject.create();
    RecordingObserver2<Integer> o = new RecordingObserver2<>();
    source.subscribe(Disposables.forObservable(lifecycle)
        .around(o));

    assertThat(source.hasObservers()).isTrue();
    assertThat(lifecycle.hasObservers()).isTrue();
    o.assertNoMoreEvents();

    source.onNext(1);
    assertThat(o.takeNext()).isEqualTo(1);

    lifecycle.onNext(2);
    source.onNext(2);
    o.assertNoMoreEvents();
    assertThat(source.hasObservers()).isFalse();
    assertThat(lifecycle.hasObservers()).isFalse();
  }

  @Test
  public void disposablesDemos() {
    PublishSubject<Integer> lifecycle = PublishSubject.create();
    SingleObserver<Integer> so = mock(SingleObserver.class);
    MaybeObserver<Integer> mo = mock(MaybeObserver.class);

    Single.just(1)
        .subscribe(Disposables.forSingle(lifecycle)
            .around(so));
    Maybe.just(1)
        .subscribe(Disposables.forMaybe(lifecycle)
            .around(mo));

    Observable.just(1)
        .subscribe(Disposables.forObservable(lifecycle)
            .around(t -> System.out.println("Hello")));
    Maybe.just(1)
        .subscribe(Disposables.forMaybe(lifecycle)
            .around(t -> System.out.println("Hello")));
    Single.just(1)
        .subscribe(Disposables.forSingle(lifecycle)
            .around(t -> System.out.println("Hello")));

    Relay<Integer> relay = PublishRelay.create();
    Observable.just(1)
        .subscribe(Disposables.forObservable(lifecycle)
            .around(relay));

    // Works for flowables and other consumers too!
    Flowable.just(1)
        .subscribe(Disposables.forFlowable(lifecycle)
            .around(relay));
    FlowableProcessor<Integer> processor = PublishProcessor.create();
    Flowable.just(1)
        .subscribe(Disposables.forFlowable(lifecycle)
            .around(processor));

    CompletableObserver co = mock(CompletableObserver.class);
    Completable.complete()
        .subscribe(Disposables.forCompletable(lifecycle)
            .around(co));
  }

  private static LifecycleProvider<Integer> makeProvider(final BehaviorSubject<Integer> lifecycle) {
    return new LifecycleProvider<Integer>() {
      @Nonnull
      @Override
      public Observable<Integer> lifecycle() {
        return lifecycle;
      }

      @Nonnull
      @Override
      public Function<Integer, Integer> correspondingEvents() {
        return CORRESPONDING_EVENTS;
      }

      @Override
      public Integer peekLifecycle() {
        return lifecycle.getValue();
      }
    };
  }
}
