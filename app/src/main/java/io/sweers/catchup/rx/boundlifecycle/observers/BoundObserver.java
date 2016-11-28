package io.sweers.catchup.rx.boundlifecycle.observers;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.CompositeException;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.plugins.RxJavaPlugins;
import io.sweers.catchup.rx.boundlifecycle.LifecycleProvider;
import java.util.concurrent.Callable;

public final class BoundObserver<T> implements Observer<T> {

  private final Observable<?> lifecycle;
  private final Consumer<? super T> consumer;
  private final Consumer<? super Throwable> errorConsumer;
  private final Action completeAction;
  private Disposable lifecycleDisposable;
  private Disposable disposable;

  private BoundObserver(@NonNull Observable<?> lifecycle,
      @Nullable Consumer<? super T> consumer,
      @Nullable Consumer<? super Throwable> errorConsumer,
      @Nullable Action completeAction) {
    this.lifecycle = lifecycle;
    this.consumer = consumer;
    this.errorConsumer = errorConsumer;
    this.completeAction = completeAction;
  }

  @Override
  public final void onSubscribe(Disposable d) {
    this.disposable = d;
    lifecycleDisposable = lifecycle.take(1)
        .subscribe(e -> d.dispose());
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
  public final void onError(Throwable e) {
    if (lifecycleDisposable != null) {
      lifecycleDisposable.dispose();
    }
    if (disposable != null) {
      disposable.dispose();
    }
    if (errorConsumer != null) {
      try {
        errorConsumer.accept(e);
      } catch (Exception e1) {
        Exceptions.throwIfFatal(e1);
        RxJavaPlugins.onError(new CompositeException(e, e1));
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

  public static class BoundObserverCreator<T> {
    private final Observable<?> lifecycle;
    private Consumer<? super T> nextConsumer;
    private Consumer<? super Throwable> errorConsumer;
    private Action completeAction;

    <E> BoundObserverCreator(@NonNull LifecycleProvider<E> provider) {
      this.lifecycle = Observable.defer(new Callable<ObservableSource<Boolean>>() {
        @Override
        public ObservableSource<Boolean> call() throws Exception {
          return LifecycleProvider.mapEvents(provider.lifecycle(), provider.correspondingEvents());
        }
      });
    }

    BoundObserverCreator(@NonNull Observable<?> lifecycle) {
      this.lifecycle = lifecycle;
    }

    public BoundObserverCreator<T> onNext(@Nullable Consumer<? super T> nextConsumer) {
      this.nextConsumer = nextConsumer;
      return this;
    }

    public BoundObserverCreator<T> onError(@Nullable Consumer<? super Throwable> errorConsumer) {
      this.errorConsumer = errorConsumer;
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
