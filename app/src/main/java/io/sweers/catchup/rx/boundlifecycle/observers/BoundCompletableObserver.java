package io.sweers.catchup.rx.boundlifecycle.observers;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import io.reactivex.CompletableObserver;
import io.reactivex.Observable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.plugins.RxJavaPlugins;
import io.sweers.catchup.rx.boundlifecycle.LifecycleProvider;

public final class BoundCompletableObserver extends BaseObserver implements CompletableObserver {

  private final Action completeAction;

  private BoundCompletableObserver(@NonNull Observable<?> lifecycle,
      @Nullable Consumer<? super Throwable> errorConsumer,
      @Nullable Action completeAction) {
    super(lifecycle, errorConsumer);
    this.completeAction = completeAction;
  }

  @Override
  public final void onComplete() {
    if (lifecycleDisposable != null) {
      lifecycleDisposable.dispose();
    }
    if (disposable != null) {
      disposable.dispose();
    }
    if (completeAction != null) {
      try {
        completeAction.run();
      } catch (Exception e) {
        Exceptions.throwIfFatal(e);
        RxJavaPlugins.onError(e);
      }
    }
  }

  public static class BoundCompletableObserverCreator
      extends BaseObserver.Creator<BoundCompletableObserverCreator> {
    private Action completeAction;

    <E> BoundCompletableObserverCreator(@NonNull LifecycleProvider<E> provider) {
      super(provider);
    }

    BoundCompletableObserverCreator(@NonNull Observable<?> lifecycle) {
      super(lifecycle);
    }

    public BoundCompletableObserverCreator onComplete(@Nullable Action completeAction) {
      this.completeAction = completeAction;
      return this;
    }

    public CompletableObserver create() {
      return new BoundCompletableObserver(lifecycle, errorConsumer, completeAction);
    }
  }
}
