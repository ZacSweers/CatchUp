package io.sweers.catchup.rx.boundlifecycle.observers;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import io.reactivex.MaybeObserver;
import io.reactivex.Observable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.plugins.RxJavaPlugins;
import io.sweers.catchup.rx.boundlifecycle.LifecycleProvider;

public final class BoundMaybeObserver<T> extends BaseObserver implements MaybeObserver<T> {

  private final Consumer<? super T> successConsumer;
  private final Action completeAction;

  private BoundMaybeObserver(@NonNull Observable<?> lifecycle,
      @Nullable Consumer<? super T> consumer,
      @Nullable Consumer<? super Throwable> errorConsumer,
      @Nullable Action completeAction) {
    super(lifecycle, errorConsumer);
    this.successConsumer = consumer;
    this.completeAction = completeAction;
  }

  @Override
  public final void onSuccess(T value) {
    if (successConsumer != null) {
      try {
        successConsumer.accept(value);
      } catch (Exception e) {
        Exceptions.throwIfFatal(e);
        onError(e);
      }
    }
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

  public static class BoundMaybeObserverCreator<T>
      extends BaseObserver.Creator<BoundMaybeObserverCreator> {
    private Consumer<? super T> successConsumer;
    private Action completeAction;

    <E> BoundMaybeObserverCreator(@NonNull LifecycleProvider<E> provider) {
      super(provider);
    }

    BoundMaybeObserverCreator(@NonNull Observable<?> lifecycle) {
      super(lifecycle);
    }

    public BoundMaybeObserverCreator<T> onSuccess(@Nullable Consumer<? super T> successConsumer) {
      this.successConsumer = successConsumer;
      return this;
    }

    public BoundMaybeObserverCreator<T> onComplete(@Nullable Action completeAction) {
      this.completeAction = completeAction;
      return this;
    }

    public MaybeObserver<T> create() {
      return new BoundMaybeObserver<>(lifecycle, successConsumer, errorConsumer, completeAction);
    }
  }
}
