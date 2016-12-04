package io.sweers.catchup.rx.boundlifecycle.observers;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.SingleObserver;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Consumer;
import io.sweers.catchup.rx.boundlifecycle.LifecycleProvider;

public final class BoundSingleObserver<T> extends BaseObserver implements SingleObserver<T> {

  private final Consumer<? super T> successConsumer;

  private BoundSingleObserver(@NonNull Maybe<?> lifecycle,
      @Nullable Consumer<? super Throwable> errorConsumer,
      @Nullable Consumer<? super T> consumer) {
    super(lifecycle, errorConsumer);
    this.successConsumer = consumer;
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

  public static class BoundSingleObserverCreator<T>
      extends BaseObserver.Creator<BoundSingleObserverCreator<T>> {
    private Consumer<? super T> successConsumer;

    <E> BoundSingleObserverCreator(@NonNull LifecycleProvider<E> provider) {
      super(provider);
    }

    BoundSingleObserverCreator(@NonNull Observable<?> lifecycle) {
      super(lifecycle);
    }

    BoundSingleObserverCreator(@NonNull Maybe<?> lifecycle) {
      super(lifecycle);
    }

    public BoundSingleObserver.BoundSingleObserverCreator<T> onSuccess(
        @Nullable Consumer<? super T> successConsumer) {
      this.successConsumer = successConsumer;
      return this;
    }

    public SingleObserver<T> asConsumer(@Nullable Consumer<? super T> nextConsumer) {
      return new BoundSingleObserver<>(lifecycle, null, nextConsumer);
    }

    public SingleObserver<T> asConsumer(@NonNull String errorTag,
        @Nullable Consumer<? super T> nextConsumer) {
      return new BoundSingleObserver<>(lifecycle, createTaggedError(errorTag), nextConsumer);
    }

    public SingleObserver<T> create() {
      return new BoundSingleObserver<>(lifecycle, errorConsumer, successConsumer);
    }
  }
}
