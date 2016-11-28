package io.sweers.catchup.rx.boundlifecycle.observers;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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

  private BoundObserver(@NonNull Observable<?> lifecycle,
      @Nullable Consumer<? super T> consumer,
      @Nullable Consumer<? super Throwable> errorConsumer,
      @Nullable Action completeAction) {
    super(lifecycle, errorConsumer);
    this.consumer = consumer;
    this.completeAction = completeAction;
  }

  @Override
  public final void onNext(T value) {
    if (consumer != null) {
      try {
        consumer.accept(value);
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

  public static class BoundObserverCreator<T>
      extends BaseObserver.Creator<BoundObserverCreator<T>> {
    private Consumer<? super T> nextConsumer;
    private Action completeAction;

    <E> BoundObserverCreator(@NonNull LifecycleProvider<E> provider) {
      super(provider);
    }

    BoundObserverCreator(@NonNull Observable<?> lifecycle) {
      super(lifecycle);
    }

    public BoundObserverCreator<T> onNext(@Nullable Consumer<? super T> nextConsumer) {
      this.nextConsumer = nextConsumer;
      return this;
    }

    public BoundObserverCreator<T> onComplete(@Nullable Action completeAction) {
      this.completeAction = completeAction;
      return this;
    }

    public Observer<T> create() {
      return new BoundObserver<>(lifecycle, nextConsumer, errorConsumer, completeAction);
    }
  }
}
