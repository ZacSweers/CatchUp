package io.sweers.catchup.rx.boundlifecycle.observers;

import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.plugins.RxJavaPlugins;
import io.sweers.catchup.rx.boundlifecycle.LifecycleProvider;

import static io.sweers.catchup.rx.boundlifecycle.observers.Util.DEFAULT_ERROR_CONSUMER;
import static io.sweers.catchup.rx.boundlifecycle.observers.Util.EMPTY_ACTION;
import static io.sweers.catchup.rx.boundlifecycle.observers.Util.createTaggedError;

public final class BoundObserver<T> extends BaseObserver implements Observer<T> {

  private final Consumer<? super T> consumer;
  private final Action completeAction;

  private BoundObserver(Maybe<?> lifecycle,
      Consumer<? super Throwable> errorConsumer,
      Consumer<? super T> consumer,
      Action completeAction) {
    super(lifecycle, errorConsumer);
    this.consumer = Util.emptyConsumerIfNull(consumer);
    this.completeAction = Util.emptyActionIfNull(completeAction);
  }

  @Override
  public final void onNext(T value) {
    try {
      consumer.accept(value);
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

    private Consumer<? super T> onNext;
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

    public Creator<T> onNext(Consumer<? super T> onNext) {
      this.onNext = onNext;
      return this;
    }

    public Creator<T> onComplete(Action completeAction) {
      this.completeAction = completeAction;
      return this;
    }

    public Observer<T> around(Consumer<? super T> onNext) {
      return around(onNext, DEFAULT_ERROR_CONSUMER, EMPTY_ACTION);
    }

    public Observer<T> around(String errorTag, Consumer<? super T> onNext) {
      return around(onNext, createTaggedError(errorTag), EMPTY_ACTION);
    }

    public Observer<T> around(Observer<T> observer) {
      return around(observer::onNext, observer::onError, observer::onComplete);
    }

    public Observer<T> around(Consumer<? super T> onNext, Consumer<? super Throwable> onError) {
      return around(onNext, onError, EMPTY_ACTION);
    }

    public Observer<T> around(Consumer<? super T> onNext,
        Consumer<? super Throwable> onError,
        Action onComplete) {
      return new BoundObserver<>(lifecycle, onError, onNext, onComplete);
    }

    public Observer<T> create() {
      return around(onNext, errorConsumer, completeAction);
    }
  }
}
