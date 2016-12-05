package io.sweers.catchup.rx.boundlifecycle.observers;

import io.reactivex.CompletableObserver;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.plugins.RxJavaPlugins;
import io.sweers.catchup.rx.boundlifecycle.LifecycleProvider;

import static io.sweers.catchup.rx.boundlifecycle.observers.Util.DEFAULT_ERROR_CONSUMER;
import static io.sweers.catchup.rx.boundlifecycle.observers.Util.createTaggedError;
import static io.sweers.catchup.rx.boundlifecycle.observers.Util.emptyActionIfNull;

final class BoundCompletableObserver extends BaseObserver implements CompletableObserver {

  private final Action completeAction;

  private BoundCompletableObserver(Maybe<?> lifecycle,
      Consumer<? super Throwable> errorConsumer,
      Action completeAction) {
    super(lifecycle, errorConsumer);
    this.completeAction = emptyActionIfNull(completeAction);
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

  public static class Creator extends BaseCreator<Creator> {

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

    public Creator onComplete(Action completeAction) {
      this.completeAction = completeAction;
      return this;
    }

    public CompletableObserver around(Action action) {
      return around(action, DEFAULT_ERROR_CONSUMER);
    }

    public CompletableObserver around(String errorTag, Action action) {
      return around(action, createTaggedError(errorTag));
    }

    public CompletableObserver around(CompletableObserver o) {
      return around(o::onComplete, o::onError);
    }

    public CompletableObserver around(Action action, Consumer<? super Throwable> onError) {
      return new BoundCompletableObserver(lifecycle, onError, action);
    }

    public CompletableObserver create() {
      return around(completeAction, errorConsumer);
    }
  }
}
