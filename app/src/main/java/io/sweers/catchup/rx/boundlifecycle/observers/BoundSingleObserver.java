package io.sweers.catchup.rx.boundlifecycle.observers;

import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.SingleObserver;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.BiConsumer;
import io.reactivex.functions.Consumer;
import io.sweers.catchup.rx.boundlifecycle.LifecycleProvider;

import static io.sweers.catchup.rx.boundlifecycle.observers.Util.DEFAULT_ERROR_CONSUMER;
import static io.sweers.catchup.rx.boundlifecycle.observers.Util.createTaggedError;
import static io.sweers.catchup.rx.boundlifecycle.observers.Util.emptyConsumerIfNull;

public final class BoundSingleObserver<T> extends BaseObserver implements SingleObserver<T> {

  private final Consumer<? super T> successConsumer;

  private BoundSingleObserver(Maybe<?> lifecycle,
      Consumer<? super Throwable> errorConsumer,
      Consumer<? super T> consumer) {
    super(lifecycle, errorConsumer);
    this.successConsumer = emptyConsumerIfNull(consumer);
  }

  @Override
  public final void onSuccess(T value) {
    try {
      successConsumer.accept(value);
    } catch (Exception e) {
      Exceptions.throwIfFatal(e);
      onError(e);
    }
  }

  public static class Creator<T> extends BaseCreator<Creator<T>> {

    private Consumer<? super T> successConsumer;

    Creator(LifecycleProvider<?> provider) {
      super(provider);
    }

    Creator(Observable<?> lifecycle) {
      super(lifecycle);
    }

    Creator(Maybe<?> lifecycle) {
      super(lifecycle);
    }

    public Creator<T> onSuccess(Consumer<? super T> successConsumer) {
      this.successConsumer = successConsumer;
      return this;
    }

    public SingleObserver<T> around(Consumer<? super T> successConsumer) {
      return around(successConsumer, DEFAULT_ERROR_CONSUMER);
    }

    public SingleObserver<T> around(String errorTag, Consumer<? super T> successConsumer) {
      return around(successConsumer, createTaggedError(errorTag));
    }

    public SingleObserver<T> around(SingleObserver<T> o) {
      return around(o::onSuccess, o::onError);
    }

    public SingleObserver<T> around(BiConsumer<? super T, ? super Throwable> o) {
      return around(v -> o.accept(v, null), t -> o.accept(null, t));
    }

    public SingleObserver<T> around(Consumer<? super T> onSuccess,
        Consumer<? super Throwable> onError) {
      return new BoundSingleObserver<>(lifecycle, onError, onSuccess);
    }

    public SingleObserver<T> create() {
      return around(successConsumer, errorConsumer);
    }
  }
}
