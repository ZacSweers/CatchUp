package io.sweers.catchup.rx.autodispose;

import com.jakewharton.rxrelay2.PublishRelay;
import com.jakewharton.rxrelay2.Relay;
import hu.akarnokd.rxjava2.subjects.MaybeSubject;
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
import io.sweers.testutils.RecordingObserver2;
import javax.annotation.Nonnull;
import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class AutoDisposeObserverTest {

  private static final Function<Integer, Integer> CORRESPONDING_EVENTS = lastEvent -> {
    switch (lastEvent) {
      case 0:
        return 3;
      case 1:
        return 2;
      default:
        throw new LifecycleEndedException();
    }
  };

  @Test
  public void autoDispose_withObservable() {
    RecordingObserver2<Integer> o = new RecordingObserver2<>();
    PublishSubject<Integer> source = PublishSubject.create();
    PublishSubject<Integer> lifecycle = PublishSubject.create();
    source.subscribe(AutoDispose.observable(lifecycle)
        .around(o));
    o.takeSubscribe();

    assertThat(source.hasObservers()).isTrue();
    assertThat(lifecycle.hasObservers()).isTrue();

    source.onNext(1);
    assertThat(o.takeNext()).isEqualTo(1);

    lifecycle.onNext(2);
    source.onNext(2);
    o.assertNoMoreEvents();
    assertThat(source.hasObservers()).isFalse();
    assertThat(lifecycle.hasObservers()).isFalse();
  }

  @Test
  public void autoDispose_withMaybe() {
    RecordingObserver2<Integer> o = new RecordingObserver2<>();
    PublishSubject<Integer> source = PublishSubject.create();
    MaybeSubject<Integer> lifecycle = MaybeSubject.create();
    source.subscribe(AutoDispose.observable(lifecycle)
        .around(o));
    o.takeSubscribe();

    assertThat(source.hasObservers()).isTrue();
    assertThat(lifecycle.hasObservers()).isTrue();

    source.onNext(1);
    assertThat(o.takeNext()).isEqualTo(1);

    lifecycle.onSuccess(2);
    source.onNext(2);
    o.assertNoMoreEvents();
    assertThat(source.hasObservers()).isFalse();
    assertThat(lifecycle.hasObservers()).isFalse();
  }

  @Test
  public void autoDispose_withProvider() {
    RecordingObserver2<Integer> o = new RecordingObserver2<>();
    PublishSubject<Integer> source = PublishSubject.create();
    BehaviorSubject<Integer> lifecycle = BehaviorSubject.createDefault(0);
    LifecycleProvider<Integer> provider = makeProvider(lifecycle);
    source.subscribe(AutoDispose.observable(provider)
        .around(o));
    o.takeSubscribe();

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
  public void autoDispose_shouldTerminateNormally_onNormalExecution() {
    RecordingObserver2<Integer> o = new RecordingObserver2<>();
    PublishSubject<Integer> source = PublishSubject.create();
    PublishSubject<Integer> lifecycle = PublishSubject.create();
    AutoDisposingObserver<Integer> auto =
        (AutoDisposingObserver<Integer>) AutoDispose.observable(lifecycle)
            .around(o);
    source.subscribe(auto);
    o.takeSubscribe();

    assertThat(source.hasObservers()).isTrue();
    assertThat(lifecycle.hasObservers()).isTrue();

    source.onNext(1);
    assertThat(o.takeNext()).isEqualTo(1);

    source.onNext(2);
    source.onComplete();
    assertThat(o.takeNext()).isEqualTo(2);
    o.assertOnComplete();
    assertThat(auto.isDisposed()).isTrue();
    assertThat(source.hasObservers()).isFalse();
    assertThat(lifecycle.hasObservers()).isFalse();
  }

  @Test
  public void autoDispose_withProvider_withoutStartingLifecycle_shouldFail() {
    BehaviorSubject<Integer> lifecycle = BehaviorSubject.create();
    RecordingObserver2<Integer> o = new RecordingObserver2<>();
    LifecycleProvider<Integer> provider = makeProvider(lifecycle);
    Observable.just(1)
        .subscribe(AutoDispose.observable(provider)
            .around(o));

    assertThat(o.takeError()).isInstanceOf(LifecycleNotStartedException.class);
  }

  @Test
  public void autoDispose_withProvider_afterLifecycle_shouldFail() {
    BehaviorSubject<Integer> lifecycle = BehaviorSubject.createDefault(0);
    lifecycle.onNext(1);
    lifecycle.onNext(2);
    lifecycle.onNext(3);
    RecordingObserver2<Integer> o = new RecordingObserver2<>();
    LifecycleProvider<Integer> provider = makeProvider(lifecycle);
    Observable.just(1)
        .subscribe(AutoDispose.observable(provider)
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
    source.subscribe(AutoDispose.observable(lifecycle)
        .empty());

    verify(cancellable, never()).cancel();
    assertThat(lifecycle.hasObservers()).isTrue();

    emitter[0].onNext(1);

    lifecycle.onNext(2);
    emitter[0].onNext(2);
    verify(cancellable).cancel();
    assertThat(lifecycle.hasObservers()).isFalse();
  }

  @Test
  @SuppressWarnings("unchecked")
  public void demos() {
    PublishSubject<Integer> lifecycle = PublishSubject.create();
    SingleObserver<Integer> so = mock(SingleObserver.class);
    MaybeObserver<Integer> mo = mock(MaybeObserver.class);

    Single.just(1)
        .subscribe(AutoDispose.single(lifecycle)
            .around(so));
    Maybe.just(1)
        .subscribe(AutoDispose.maybe(lifecycle)
            .around(mo));

    Observable.just(1)
        .subscribe(AutoDispose.observable(lifecycle)
            .around(t -> System.out.println("Hello")));
    Maybe.just(1)
        .subscribe(AutoDispose.maybe(lifecycle)
            .around(t -> System.out.println("Hello")));
    Single.just(1)
        .subscribe(AutoDispose.single(lifecycle)
            .around(t -> System.out.println("Hello")));

    Relay<Integer> relay = PublishRelay.create();
    Observable.just(1)
        .subscribe(AutoDispose.observable(lifecycle)
            .around(relay));

    // Works flowables and other consumers too!
    Flowable.just(1)
        .subscribe(AutoDispose.flowable(lifecycle)
            .around(relay));
    FlowableProcessor<Integer> processor = PublishProcessor.create();
    Flowable.just(1)
        .subscribe(AutoDispose.flowable(lifecycle)
            .around(processor));

    CompletableObserver co = mock(CompletableObserver.class);
    Completable.complete()
        .subscribe(AutoDispose.completable(lifecycle)
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
