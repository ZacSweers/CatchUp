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

    public SingleObserver<T> asConsumer(Consumer<? super T> successConsumer) {
      return new BoundSingleObserver<>(lifecycle, DEFAULT_ERROR_CONSUMER, successConsumer);
    }

    public SingleObserver<T> asConsumer(String errorTag, Consumer<? super T> successConsumer) {
      return new BoundSingleObserver<>(lifecycle, createTaggedError(errorTag), successConsumer);
    }

    public SingleObserver<T> around(SingleObserver<T> o) {
      return new BoundSingleObserver<>(lifecycle, o::onError, o::onSuccess);
    }

    public SingleObserver<T> around(BiConsumer<? super T, ? super Throwable> o) {
      return new BoundSingleObserver<>(lifecycle, t -> o.accept(null, t), v -> o.accept(v, null));
    }

    public SingleObserver<T> create() {
      return new BoundSingleObserver<>(lifecycle, errorConsumer, successConsumer);
    }
  }
}
