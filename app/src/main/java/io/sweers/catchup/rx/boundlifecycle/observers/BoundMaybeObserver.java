package io.sweers.catchup.rx.boundlifecycle.observers;

import io.reactivex.Maybe;
import io.reactivex.MaybeObserver;
import io.reactivex.Observable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.plugins.RxJavaPlugins;
import io.sweers.catchup.rx.boundlifecycle.LifecycleProvider;

import static io.sweers.catchup.rx.boundlifecycle.observers.Util.DEFAULT_ERROR_CONSUMER;
import static io.sweers.catchup.rx.boundlifecycle.observers.Util.EMPTY_ACTION;
import static io.sweers.catchup.rx.boundlifecycle.observers.Util.createTaggedError;
import static io.sweers.catchup.rx.boundlifecycle.observers.Util.emptyActionIfNull;
import static io.sweers.catchup.rx.boundlifecycle.observers.Util.emptyConsumerIfNull;

final class BoundMaybeObserver<T> extends BaseObserver implements MaybeObserver<T> {

  private final Consumer<? super T> onSuccess;
  private final Action onComplete;

  private BoundMaybeObserver(Maybe<?> lifecycle,
      Consumer<? super Throwable> errorConsumer,
      Consumer<? super T> consumer,
      Action onComplete) {
    super(lifecycle, errorConsumer);
    this.onSuccess = emptyConsumerIfNull(consumer);
    this.onComplete = emptyActionIfNull(onComplete);
  }

  @Override
  public final void onSuccess(T value) {
    try {
      onSuccess.accept(value);
    } catch (Exception e) {
      Exceptions.throwIfFatal(e);
      onError(e);
    }
  }

  @Override
  public final void onComplete() {
    try {
      onComplete.run();
    } catch (Exception e) {
      Exceptions.throwIfFatal(e);
      RxJavaPlugins.onError(e);
    }
  }

  public static class Creator<T> extends BaseCreator<Creator<T>> {

    private Consumer<? super T> onSuccess;
    private Action onComplete;

    Creator(LifecycleProvider<?> provider) {
      super(provider);
    }

    Creator(Observable<?> lifecycle) {
      super(lifecycle);
    }

    Creator(Maybe<?> lifecycle) {
      super(lifecycle);
    }

    public Creator<T> onSuccess(Consumer<? super T> onSuccess) {
      this.onSuccess = onSuccess;
      return this;
    }

    public Creator<T> onComplete(Action onComplete) {
      this.onComplete = onComplete;
      return this;
    }

    public MaybeObserver<T> around(Consumer<? super T> nextConsumer) {
      return around(nextConsumer, DEFAULT_ERROR_CONSUMER, EMPTY_ACTION);
    }

    public MaybeObserver<T> around(String errorTag, Consumer<? super T> nextConsumer) {
      return around(nextConsumer, createTaggedError(errorTag), EMPTY_ACTION);
    }

    public MaybeObserver<T> around(Consumer<? super T> onSuccess,
        Consumer<? super Throwable> onError) {
      return around(onSuccess, onError, EMPTY_ACTION);
    }

    public MaybeObserver<T> around(MaybeObserver<T> o) {
      return around(o::onSuccess, o::onError, o::onComplete);
    }

    public MaybeObserver<T> around(Consumer<? super T> onSuccess,
        Consumer<? super Throwable> onError,
        Action onComplete) {
      return new BoundMaybeObserver<>(lifecycle, onError, onSuccess, onComplete);
    }

    public MaybeObserver<T> create() {
      return around(onSuccess, errorConsumer, onComplete);
    }
  }
}
