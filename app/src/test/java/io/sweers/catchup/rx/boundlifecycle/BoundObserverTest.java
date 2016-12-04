package io.sweers.catchup.rx.boundlifecycle;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.functions.Cancellable;
import io.reactivex.functions.Function;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;
import io.sweers.catchup.rx.boundlifecycle.observers.BoundObservers;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nonnull;
import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class BoundObserverTest {

  //public interface Receiver<T> {
  //  void onReceive(T t);
  //}
  //
  //public static class Doer<T> {
  //  public void doSomething(Receiver<T> receiver) {
  //    // Stuff
  //  }
  //}
  //
  //public static class ReceiverHelper {
  //  public static ReceiverHelper help() {
  //    return new ReceiverHelper();
  //  }
  //
  //  public <T> ReceiverCreator<T> creator() {
  //    return new ReceiverCreator<>();
  //  }
  //}
  //
  //public static class ParameterizedReceiverHelper<T> {
  //
  //  public static <T> ParameterizedReceiverHelper<T> make() {
  //    return new ParameterizedReceiverHelper<>();
  //  }
  //
  //  public <E> ReceiverCreator<E> creator() {
  //    return new ReceiverCreator<>();
  //  }
  //}
  //
  //public static <T> Receiver<T> directReceiver() {
  //  return new ReceiverCreator<T>().create();
  //}
  //
  //public static <T> Receiver<T> directStaticReceiver() {
  //  return ReceiverCreator.staticCreate();
  //}
  //
  //public static <T> ReceiverCreator<T> directReceiverCreator() {
  //  return new ReceiverCreator<>();
  //}
  //
  //public static class ReceiverCreator<E> {
  //
  //  public static <E> Receiver<E> staticCreate() {
  //    return new ReceiverCreator<E>().create();
  //  }
  //
  //  public static <E> ReceiverCreator<E> staticCreator() {
  //    return new ReceiverCreator<E>();
  //  }
  //
  //  public Receiver<E> create() {
  //    return t -> { };
  //  }
  //}
  //
  //public void tesReceiver() {
  //  Doer<Integer> doer = new Doer<>();
  //
  //  doer.doSomething(directReceiver()); // Fine
  //  doer.doSomething(new ReceiverCreator().create()); // Result type erased
  //  doer.doSomething(new ReceiverCreator<>().create()); // Compile error, returns Receiver<Object>
  //  doer.doSomething(new ReceiverCreator<Integer>().create()); // Fine
  //  doer.doSomething(ReceiverCreator.staticCreate()); // Fine
  //  doer.doSomething(ReceiverCreator.staticCreator().create()); // Compile error, returns Receiver<Object>
  //  doer.doSomething(directStaticReceiver()); // Fine
  //  doer.doSomething(directReceiverCreator().create()); // Compile error, returns Receiver<Object>
  //  doer.doSomething(BoundObserverTest.<Integer>directReceiverCreator().create()); // Fine
  //  doer.doSomething(ReceiverHelper.help().creator().create()); // Compile error, returns Receiver<Object>
  //  doer.doSomething(ParameterizedReceiverHelper.make().creator().create()); // Compile error, returns Receiver<Object>
  //}

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
    AtomicInteger valHolder = new AtomicInteger(0);
    PublishSubject<Integer> source = PublishSubject.create();
    BehaviorSubject<Integer> lifecycle = BehaviorSubject.createDefault(0);
    Function<Integer, Integer> correspondingEvents = integer -> {
      int intVal = integer;
      switch (intVal) {
        case 0:
          return 3;
        case 1:
          return 2;
        default:
          throw new RuntimeException("Out of lifecycle");
      }
    };
    LifecycleProvider<Integer> provider = new LifecycleProvider<Integer>() {
      @Nonnull
      @Override
      public Observable<Integer> lifecycle() {
        return lifecycle;
      }

      @Nonnull
      @Override
      public Function<Integer, Integer> correspondingEvents() {
        return correspondingEvents;
      }

      @Override
      public boolean hasLifecycleStarted() {
        return lifecycle.getValue() != null;
      }
    };
    source.subscribe(BoundObservers.<Integer>forObservable(provider).onNext(valHolder::set)
        .create());

    assertThat(source.hasObservers()).isTrue();
    assertThat(lifecycle.hasObservers()).isTrue();

    source.onNext(1);
    assertThat(valHolder.get()).isEqualTo(1);

    lifecycle.onNext(1);
    source.onNext(2);

    assertThat(source.hasObservers()).isTrue();
    assertThat(lifecycle.hasObservers()).isTrue();
    assertThat(valHolder.get()).isEqualTo(2);

    lifecycle.onNext(3);
    source.onNext(3);

    assertThat(valHolder.get()).isEqualTo(2);
    assertThat(source.hasObservers()).isFalse();
    assertThat(lifecycle.hasObservers()).isFalse();
  }

  @Test
  public void verifyCancellation() throws Exception {
    Cancellable cancellable = mock(Cancellable.class);
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
}
