package io.sweers.catchup.rx.boundlifecycle;

import io.reactivex.Observable;
import io.reactivex.functions.Function;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;
import io.sweers.catchup.rx.boundlifecycle.observers.BoundObservers;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nonnull;
import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;

public class BoundObserverTest {

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
}
