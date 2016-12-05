package io.sweers.catchup.rx.boundlifecycle.observers;

import android.support.annotation.Nullable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.functions.Action;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.plugins.RxJavaPlugins;
import io.sweers.catchup.rx.boundlifecycle.LifecycleProvider;
import rx.exceptions.OnErrorNotImplementedException;

class Util {

  private static final BiFunction<Object, Object, Boolean> COMPARATOR =
      (BiFunction<Object, Object, Boolean>) Object::equals;
  private static final Function<Object, LifecycleEvent> TRANSFORM_TO_END = o -> LifecycleEvent.END;

  static final Action EMPTY_ACTION = new Action() {
    @Override
    public void run() { }

    @Override
    public String toString() {
      return "BoundEmptyAction";
    }
  };

  static final Consumer<Object> EMPTY_CONSUMER = new Consumer<Object>() {
    @Override
    public void accept(Object v) { }

    @Override
    public String toString() {
      return "BoundEmptyConsumer";
    }
  };

  static final Consumer<Throwable> DEFAULT_ERROR_CONSUMER = new Consumer<Throwable>() {
    @Override
    public void accept(Throwable throwable) throws Exception {
      RxJavaPlugins.onError(throwable);
    }

    @Override
    public String toString() {
      return "BoundEmptyErrorConsumer";
    }
  };

  static <T> T checkNotNull(@Nullable T value) {
    return checkNotNull(value, "value == null");
  }

  static <T> T checkNotNull(@Nullable T value, String message) {
    if (value == null) {
      throw new NullPointerException(message);
    } else {
      return value;
    }
  }

  static <E> Maybe<LifecycleEvent> mapEvents(LifecycleProvider<E> provider) {
    return mapEvents(provider.lifecycle(), provider.correspondingEvents());
  }

  static <E> Maybe<LifecycleEvent> mapEvents(Observable<E> lifecycle,
      Function<E, E> correspondingEvents) {
    return Observable.combineLatest(lifecycle.take(1)
        .map(correspondingEvents), lifecycle.skip(1), COMPARATOR)
        .filter(b -> b)
        .map(TRANSFORM_TO_END)
        .firstElement();
  }

  /**
   * Returns an empty consumer that does nothing.
   *
   * @param <T> the consumed value type, the value is ignored
   * @return an empty consumer that does nothing.
   */
  @SuppressWarnings("unchecked")
  static <T> Consumer<T> emptyConsumer() {
    return (Consumer<T>) EMPTY_CONSUMER;
  }

  static <E> Consumer<E> emptyConsumerIfNull(@Nullable Consumer<E> c) {
    return c != null ? c : emptyConsumer();
  }

  static Consumer<? super Throwable> emptyErrorConsumerIfNull(
      @Nullable Consumer<? super Throwable> c) {
    return c != null ? c : DEFAULT_ERROR_CONSUMER;
  }

  static Action emptyActionIfNull(@Nullable Action a) {
    return a != null ? a : EMPTY_ACTION;
  }

  static Consumer<? super Throwable> createTaggedError(String tag) {
    return (Consumer<Throwable>) throwable -> {
      throw new OnErrorNotImplementedException(tag, throwable);
    };
  }

  enum LifecycleEvent {
    END
  }
}
