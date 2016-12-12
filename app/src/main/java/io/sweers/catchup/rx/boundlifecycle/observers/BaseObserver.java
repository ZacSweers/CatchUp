package io.sweers.catchup.rx.boundlifecycle.observers;

import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.CompositeException;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Consumer;
import io.reactivex.internal.disposables.DisposableHelper;
import io.reactivex.plugins.RxJavaPlugins;
import io.sweers.catchup.rx.boundlifecycle.LifecycleProvider;
import java.util.concurrent.atomic.AtomicReference;

import static io.sweers.catchup.rx.boundlifecycle.observers.Util.checkNotNull;
import static io.sweers.catchup.rx.boundlifecycle.observers.Util.deferredResolvedLifecycle;
import static io.sweers.catchup.rx.boundlifecycle.observers.Util.emptyErrorConsumerIfNull;

abstract class BaseObserver implements Disposable {

  private final AtomicReference<Disposable> mainDisposable = new AtomicReference<>();
  private final AtomicReference<Disposable> lifecycleDisposable = new AtomicReference<>();
  protected final Maybe<?> lifecycle;
  protected final Consumer<? super Throwable> errorConsumer;

  protected BaseObserver(Maybe<?> lifecycle, Consumer<? super Throwable> errorConsumer) {
    this.lifecycle = lifecycle;
    this.errorConsumer = emptyErrorConsumerIfNull(errorConsumer);
  }

  @SuppressWarnings("unused")
  public final void onSubscribe(Disposable d) {
    if (DisposableHelper.setOnce(this.mainDisposable, d)) {
      DisposableHelper.setOnce(this.lifecycleDisposable,
          lifecycle.subscribe(e -> dispose(), t -> {
            dispose();
            onError(t);
          }));
    }
  }

  @Override
  public final boolean isDisposed() {
    return mainDisposable.get() == DisposableHelper.DISPOSED;
  }

  @Override
  public final void dispose() {
    synchronized (this) {
      DisposableHelper.dispose(lifecycleDisposable);
      DisposableHelper.dispose(mainDisposable);
    }
  }

  public final void onError(Throwable e) {
    try {
      errorConsumer.accept(e);
    } catch (Exception e1) {
      Exceptions.throwIfFatal(e1);
      RxJavaPlugins.onError(new CompositeException(e, e1));
    }
  }

  abstract static class BaseCreator<C extends BaseCreator> {
    protected final Maybe<?> lifecycle;
    protected Consumer<? super Throwable> errorConsumer;

    protected <E> BaseCreator(LifecycleProvider<E> provider) {
      this.lifecycle = deferredResolvedLifecycle(provider);
    }

    protected <E> BaseCreator(Observable<E> lifecycle) {
      this.lifecycle = checkNotNull(lifecycle, "lifecycle == null").firstElement();
    }

    protected <E> BaseCreator(Maybe<E> lifecycle) {
      this.lifecycle = checkNotNull(lifecycle, "lifecycle == null");
    }

    @SuppressWarnings("unchecked")
    public C onError(Consumer<? super Throwable> errorConsumer) {
      this.errorConsumer = errorConsumer;
      return (C) this;
    }
  }
}
