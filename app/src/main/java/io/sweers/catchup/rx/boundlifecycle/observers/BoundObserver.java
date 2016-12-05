package io.sweers.catchup.rx.boundlifecycle.observers;

import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.plugins.RxJavaPlugins;
import io.sweers.catchup.rx.boundlifecycle.LifecycleProvider;

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

    private Consumer<? super T> nextConsumer;
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

    public Creator<T> onNext(Consumer<? super T> nextConsumer) {
      this.nextConsumer = nextConsumer;
      return this;
    }

    public Creator<T> onComplete(Action completeAction) {
      this.completeAction = completeAction;
      return this;
    }

    public Observer<T> asConsumer(Consumer<? super T> nextConsumer) {
      return new BoundObserver<>(lifecycle, Util.DEFAULT_ERROR_CONSUMER, nextConsumer, Util.EMPTY_ACTION);
    }

    public Observer<T> asConsumer(String errorTag, Consumer<? super T> nextConsumer) {
      return new BoundObserver<>(lifecycle,
          Util.createTaggedError(errorTag),
          nextConsumer,
          Util.EMPTY_ACTION);
    }

    public Observer<T> around(Observer<T> observer) {
      return new BoundObserver<>(lifecycle,
          observer::onError,
          observer::onNext,
          observer::onComplete);
    }

    public Observer<T> create() {
      return new BoundObserver<>(lifecycle, errorConsumer, nextConsumer, completeAction);
    }
  }
}
