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

  private final Consumer<? super T> successConsumer;
  private final Action completeAction;

  private BoundMaybeObserver(Maybe<?> lifecycle,
      Consumer<? super Throwable> errorConsumer,
      Consumer<? super T> consumer,
      Action completeAction) {
    super(lifecycle, errorConsumer);
    this.successConsumer = emptyConsumerIfNull(consumer);
    this.completeAction = emptyActionIfNull(completeAction);
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

  @Override
  public final void onComplete() {
    try {
      completeAction.run();
    } catch (Exception e) {
      Exceptions.throwIfFatal(e);
      RxJavaPlugins.onError(e);
    }
  }

  public static class Creator<T> extends BaseCreator<Creator<T>> {

    private Consumer<? super T> successConsumer;
    private Action completeAction;

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

    public Creator<T> onComplete(Action completeAction) {
      this.completeAction = completeAction;
      return this;
    }

    public MaybeObserver<T> asConsumer(Consumer<? super T> nextConsumer) {
      return new BoundMaybeObserver<>(lifecycle,
          DEFAULT_ERROR_CONSUMER,
          nextConsumer,
          EMPTY_ACTION);
    }

    public MaybeObserver<T> asConsumer(String errorTag, Consumer<? super T> nextConsumer) {
      return new BoundMaybeObserver<>(lifecycle,
          createTaggedError(errorTag),
          nextConsumer,
          EMPTY_ACTION);
    }

    public MaybeObserver<T> around(MaybeObserver<T> o) {
      return new BoundMaybeObserver<>(lifecycle, o::onError, o::onSuccess, o::onComplete);
    }

    public MaybeObserver<T> create() {
      return new BoundMaybeObserver<>(lifecycle, errorConsumer, successConsumer, completeAction);
    }
  }
}
