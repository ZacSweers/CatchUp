package io.sweers.catchup.rx.boundlifecycle.observers;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import io.reactivex.Maybe;
import io.reactivex.MaybeObserver;
import io.reactivex.Observable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.plugins.RxJavaPlugins;
import io.sweers.catchup.rx.boundlifecycle.LifecycleProvider;

final class BoundMaybeObserver<T> extends BaseObserver implements MaybeObserver<T> {

  private final Consumer<? super T> successConsumer;
  private final Action completeAction;

  private BoundMaybeObserver(@NonNull Maybe<?> lifecycle,
      @Nullable Consumer<? super Throwable> errorConsumer,
      @Nullable Consumer<? super T> consumer,
      @Nullable Action completeAction) {
    super(lifecycle, errorConsumer);
    this.successConsumer = consumer;
    this.completeAction = completeAction;
  }

  @Override
  public final void onSuccess(T value) {
    dispose();
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
    dispose();
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
      extends BaseObserver.Creator<BoundMaybeObserverCreator<T>> {
    private Consumer<? super T> successConsumer;
    private Action completeAction;

    <E> BoundMaybeObserverCreator(@NonNull LifecycleProvider<E> provider) {
      super(provider);
    }

    BoundMaybeObserverCreator(@NonNull Observable<?> lifecycle) {
      super(lifecycle);
    }

    BoundMaybeObserverCreator(@NonNull Maybe<?> lifecycle) {
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

    public MaybeObserver<T> asConsumer(@Nullable Consumer<? super T> nextConsumer) {
      return new BoundMaybeObserver<>(lifecycle, null, nextConsumer, null);
    }

    public MaybeObserver<T> asConsumer(@NonNull String errorTag,
        @Nullable Consumer<? super T> nextConsumer) {
      return new BoundMaybeObserver<>(lifecycle, createTaggedError(errorTag), nextConsumer, null);
    }

    public MaybeObserver<T> create() {
      return new BoundMaybeObserver<>(lifecycle, errorConsumer, successConsumer, completeAction);
    }
  }
}
