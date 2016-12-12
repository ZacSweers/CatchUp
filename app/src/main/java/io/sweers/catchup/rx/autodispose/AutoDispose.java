package io.sweers.catchup.rx.autodispose;

import io.reactivex.CompletableObserver;
import io.reactivex.Maybe;
import io.reactivex.MaybeObserver;
import io.reactivex.MaybeSource;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.SingleObserver;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.BiConsumer;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.sweers.catchup.rx.autodispose.internal.AutoDisposeUtil;
import java.util.concurrent.Callable;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public final class AutoDispose {

  private static final Function<Object, LifecycleEndEvent> TRANSFORM_TO_END =
      o -> LifecycleEndEvent.INSTANCE;

  private AutoDispose() {
    throw new InstantiationError();
  }

  public static Subscribers flowable(LifecycleProvider<?> provider) {
    return new Subscribers(provider);
  }

  public static Subscribers flowable(Observable<?> lifecycle) {
    return new Subscribers(lifecycle);
  }

  public static Subscribers flowable(Maybe<?> lifecycle) {
    return new Subscribers(lifecycle);
  }

  public static Observers observable(LifecycleProvider<?> provider) {
    return new Observers(provider);
  }

  public static Observers observable(Observable<?> lifecycle) {
    return new Observers(lifecycle);
  }

  public static Observers observable(Maybe<?> lifecycle) {
    return new Observers(lifecycle);
  }

  public static SingleObservers single(LifecycleProvider<?> provider) {
    return new SingleObservers(provider);
  }

  public static SingleObservers single(Observable<?> lifecycle) {
    return new SingleObservers(lifecycle);
  }

  public static SingleObservers single(Maybe<?> lifecycle) {
    return new SingleObservers(lifecycle);
  }

  public static MaybeObservers maybe(LifecycleProvider<?> provider) {
    return new MaybeObservers(provider);
  }

  public static MaybeObservers maybe(Observable<?> lifecycle) {
    return new MaybeObservers(lifecycle);
  }

  public static MaybeObservers maybe(Maybe<?> lifecycle) {
    return new MaybeObservers(lifecycle);
  }

  public static CompletableObservers completable(LifecycleProvider<?> provider) {
    return new CompletableObservers(provider);
  }

  public static CompletableObservers completable(Observable<?> lifecycle) {
    return new CompletableObservers(lifecycle);
  }

  public static CompletableObservers completable(Maybe<?> lifecycle) {
    return new CompletableObservers(lifecycle);
  }

  private static <E> Maybe<LifecycleEndEvent> deferredResolvedLifecycle(LifecycleProvider<E> provider) {
    return Maybe.defer(new Callable<MaybeSource<LifecycleEndEvent>>() {
      @Override
      public MaybeSource<LifecycleEndEvent> call() throws Exception {
        E lastEvent = provider.peekLifecycle();
        if (lastEvent == null) {
          throw new LifecycleNotStartedException();
        }
        E endEvent = provider.correspondingEvents()
            .apply(lastEvent);
        return mapEvents(provider.lifecycle(), endEvent);
      }
    });
  }

  private static <E> Maybe<LifecycleEndEvent> mapEvents(Observable<E> lifecycle, E endEvent) {
    return lifecycle.skip(1)
        .map(e -> e.equals(endEvent))
        .filter(b -> b)
        .map(TRANSFORM_TO_END)
        .firstElement();
  }

  private enum LifecycleEndEvent {
    INSTANCE
  }

  private static class Base {
    protected final Maybe<?> lifecycle;

    protected Base(LifecycleProvider<?> provider) {
      this(deferredResolvedLifecycle(AutoDisposeUtil.checkNotNull(provider, "provider == null")));
    }

    protected Base(Observable<?> lifecycle) {
      this(AutoDisposeUtil.checkNotNull(lifecycle, "lifecycle == null").firstElement());
    }

    protected Base(Maybe<?> lifecycle) {
      this.lifecycle = AutoDisposeUtil.checkNotNull(lifecycle, "lifecycle == null");
    }
  }

  public static class Subscribers extends Base {
    private Subscribers(LifecycleProvider<?> provider) {
      super(provider);
    }

    private Subscribers(Observable<?> lifecycle) {
      super(lifecycle);
    }

    private Subscribers(Maybe<?> lifecycle) {
      super(lifecycle);
    }

    public <T> Subscriber<T> empty() {
      return around(AutoDisposeUtil.EMPTY_CONSUMER,
          AutoDisposeUtil.DEFAULT_ERROR_CONSUMER,
          AutoDisposeUtil.EMPTY_ACTION);
    }

    public <T> Subscriber<T> around(Consumer<? super T> onNext) {
      AutoDisposeUtil.checkNotNull(onNext, "onNext == null");
      return around(onNext, AutoDisposeUtil.DEFAULT_ERROR_CONSUMER, AutoDisposeUtil.EMPTY_ACTION);
    }

    public <T> Subscriber<T> around(Consumer<? super T> onNext,
        Consumer<? super Throwable> onError) {
      AutoDisposeUtil.checkNotNull(onNext, "onNext == null");
      AutoDisposeUtil.checkNotNull(onError, "onError == null");
      return around(onNext, onError, AutoDisposeUtil.EMPTY_ACTION);
    }

    public <T> Subscriber<T> around(Consumer<? super T> onNext,
        Consumer<? super Throwable> onError,
        Action onComplete) {
      AutoDisposeUtil.checkNotNull(onNext, "onNext == null");
      AutoDisposeUtil.checkNotNull(onError, "onError == null");
      AutoDisposeUtil.checkNotNull(onComplete, "onComplete == null");
      return around(onNext, onError, onComplete, AutoDisposeUtil.EMPTY_SUBSCRIPTION_CONSUMER);
    }

    public <T> Subscriber<T> around(Subscriber<T> subscriber) {
      AutoDisposeUtil.checkNotNull(subscriber, "subscriber == null");
      return around(subscriber::onNext,
          subscriber::onError,
          subscriber::onComplete,
          subscriber::onSubscribe);
    }

    public <T> Subscriber<T> around(Consumer<? super T> onNext,
        Consumer<? super Throwable> onError,
        Action onComplete,
        Consumer<? super Subscription> onSubscribe) {
      AutoDisposeUtil.checkNotNull(onNext, "onNext == null");
      AutoDisposeUtil.checkNotNull(onError, "onError == null");
      AutoDisposeUtil.checkNotNull(onComplete, "onComplete == null");
      AutoDisposeUtil.checkNotNull(onSubscribe, "onSubscribe == null");
      return new AutoDisposingSubscriber<>(lifecycle, onNext, onError, onComplete, onSubscribe);
    }
  }

  public static class Observers extends Base {
    private Observers(LifecycleProvider<?> provider) {
      super(provider);
    }

    private Observers(Observable<?> lifecycle) {
      super(lifecycle);
    }

    private Observers(Maybe<?> lifecycle) {
      super(lifecycle);
    }

    public <T> Observer<T> empty() {
      return around(AutoDisposeUtil.EMPTY_CONSUMER);
    }

    public <T> Observer<T> around(Consumer<? super T> onNext) {
      AutoDisposeUtil.checkNotNull(onNext, "onNext == null");
      return around(onNext, AutoDisposeUtil.DEFAULT_ERROR_CONSUMER, AutoDisposeUtil.EMPTY_ACTION);
    }

    public <T> Observer<T> around(Consumer<? super T> onNext, Consumer<? super Throwable> onError) {
      AutoDisposeUtil.checkNotNull(onNext, "onNext == null");
      AutoDisposeUtil.checkNotNull(onError, "onError == null");
      return around(onNext, onError, AutoDisposeUtil.EMPTY_ACTION);
    }

    public <T> Observer<T> around(Consumer<? super T> onNext,
        Consumer<? super Throwable> onError,
        Action onComplete) {
      AutoDisposeUtil.checkNotNull(onNext, "onNext == null");
      AutoDisposeUtil.checkNotNull(onError, "onError == null");
      AutoDisposeUtil.checkNotNull(onComplete, "onComplete == null");
      return around(onNext, onError, onComplete, AutoDisposeUtil.EMPTY_DISPOSABLE_CONSUMER);
    }

    public <T> Observer<T> around(Observer<T> observer) {
      AutoDisposeUtil.checkNotNull(observer, "observer == null");
      return around(observer::onNext,
          observer::onError,
          observer::onComplete,
          observer::onSubscribe);
    }

    public <T> Observer<T> around(Consumer<? super T> onNext,
        Consumer<? super Throwable> onError,
        Action onComplete,
        Consumer<? super Disposable> onSubscribe) {
      AutoDisposeUtil.checkNotNull(onNext, "onNext == null");
      AutoDisposeUtil.checkNotNull(onError, "onError == null");
      AutoDisposeUtil.checkNotNull(onComplete, "onComplete == null");
      AutoDisposeUtil.checkNotNull(onSubscribe, "onSubscribe == null");
      return new AutoDisposingObserver<>(lifecycle, onNext, onError, onComplete, onSubscribe);
    }
  }

  public static class SingleObservers extends Base {
    private SingleObservers(LifecycleProvider<?> provider) {
      super(provider);
    }

    private SingleObservers(Observable<?> lifecycle) {
      super(lifecycle);
    }

    private SingleObservers(Maybe<?> lifecycle) {
      super(lifecycle);
    }

    public <T> SingleObserver<T> empty() {
      return around(AutoDisposeUtil.EMPTY_CONSUMER);
    }

    public <T> SingleObserver<T> around(Consumer<? super T> onSuccess) {
      AutoDisposeUtil.checkNotNull(onSuccess, "onSuccess == null");
      return around(onSuccess, AutoDisposeUtil.DEFAULT_ERROR_CONSUMER);
    }

    public <T> SingleObserver<T> around(BiConsumer<? super T, ? super Throwable> biConsumer) {
      AutoDisposeUtil.checkNotNull(biConsumer, "biConsumer == null");
      return around(v -> biConsumer.accept(v, null), t -> biConsumer.accept(null, t));
    }

    public <T> SingleObserver<T> around(Consumer<? super T> onSuccess,
        Consumer<? super Throwable> onError) {
      AutoDisposeUtil.checkNotNull(onSuccess, "onSuccess == null");
      AutoDisposeUtil.checkNotNull(onError, "onError == null");
      return around(onSuccess, onError, AutoDisposeUtil.EMPTY_DISPOSABLE_CONSUMER);
    }

    public <T> SingleObserver<T> around(SingleObserver<T> observer) {
      AutoDisposeUtil.checkNotNull(observer, "observer == null");
      return around(observer::onSuccess, observer::onError, observer::onSubscribe);
    }

    public <T> SingleObserver<T> around(Consumer<? super T> onSuccess,
        Consumer<? super Throwable> onError,
        Consumer<? super Disposable> onSubscribe) {
      AutoDisposeUtil.checkNotNull(onSuccess, "onSuccess == null");
      AutoDisposeUtil.checkNotNull(onError, "onError == null");
      AutoDisposeUtil.checkNotNull(onSubscribe, "onSubscribe == null");
      return new AutoDisposingSingleObserver<>(lifecycle, onSuccess, onError, onSubscribe);
    }
  }

  public static class MaybeObservers extends Base {
    private MaybeObservers(LifecycleProvider<?> provider) {
      super(provider);
    }

    private MaybeObservers(Observable<?> lifecycle) {
      super(lifecycle);
    }

    private MaybeObservers(Maybe<?> lifecycle) {
      super(lifecycle);
    }

    public <T> MaybeObserver<T> empty() {
      return around(AutoDisposeUtil.EMPTY_CONSUMER);
    }

    public <T> MaybeObserver<T> around(Consumer<? super T> onSuccess) {
      AutoDisposeUtil.checkNotNull(onSuccess, "onSuccess == null");
      return around(onSuccess,
          AutoDisposeUtil.DEFAULT_ERROR_CONSUMER,
          AutoDisposeUtil.EMPTY_ACTION);
    }

    public <T> MaybeObserver<T> around(Consumer<? super T> onSuccess,
        Consumer<? super Throwable> onError) {
      AutoDisposeUtil.checkNotNull(onSuccess, "onSuccess == null");
      AutoDisposeUtil.checkNotNull(onError, "onError == null");
      return around(onSuccess, onError, AutoDisposeUtil.EMPTY_ACTION);
    }

    public <T> MaybeObserver<T> around(Consumer<? super T> onSuccess,
        Consumer<? super Throwable> onError,
        Action onComplete) {
      AutoDisposeUtil.checkNotNull(onSuccess, "onSuccess == null");
      AutoDisposeUtil.checkNotNull(onError, "onError == null");
      AutoDisposeUtil.checkNotNull(onComplete, "onComplete == null");
      return around(onSuccess, onError, onComplete, AutoDisposeUtil.EMPTY_DISPOSABLE_CONSUMER);
    }

    public <T> MaybeObserver<T> around(MaybeObserver<T> observer) {
      AutoDisposeUtil.checkNotNull(observer, "observer == null");
      return around(observer::onSuccess, observer::onError, observer::onComplete);
    }

    public <T> MaybeObserver<T> around(Consumer<? super T> onSuccess,
        Consumer<? super Throwable> onError,
        Action onComplete,
        Consumer<? super Disposable> onSubscribe) {
      AutoDisposeUtil.checkNotNull(onSuccess, "onSuccess == null");
      AutoDisposeUtil.checkNotNull(onError, "onError == null");
      AutoDisposeUtil.checkNotNull(onComplete, "onComplete == null");
      AutoDisposeUtil.checkNotNull(onSubscribe, "onSubscribe == null");
      return new AutoDisposingMaybeObserver<>(lifecycle,
          onSuccess,
          onError,
          onComplete,
          onSubscribe);
    }
  }

  public static class CompletableObservers extends Base {
    private CompletableObservers(LifecycleProvider<?> provider) {
      super(provider);
    }

    private CompletableObservers(Observable<?> lifecycle) {
      super(lifecycle);
    }

    private CompletableObservers(Maybe<?> lifecycle) {
      super(lifecycle);
    }

    public CompletableObserver empty() {
      return around(AutoDisposeUtil.EMPTY_ACTION);
    }

    public CompletableObserver around(Action action) {
      AutoDisposeUtil.checkNotNull(action, "action == null");
      return around(action, AutoDisposeUtil.DEFAULT_ERROR_CONSUMER);
    }

    public CompletableObserver around(Action action, Consumer<? super Throwable> onError) {
      AutoDisposeUtil.checkNotNull(action, "action == null");
      AutoDisposeUtil.checkNotNull(onError, "onError == null");
      return around(action, onError, AutoDisposeUtil.EMPTY_DISPOSABLE_CONSUMER);
    }

    public CompletableObserver around(CompletableObserver observer) {
      AutoDisposeUtil.checkNotNull(observer, "observer == null");
      return around(observer::onComplete, observer::onError);
    }

    public CompletableObserver around(Action action,
        Consumer<? super Throwable> onError,
        Consumer<? super Disposable> onSubscribe) {
      AutoDisposeUtil.checkNotNull(action, "action == null");
      AutoDisposeUtil.checkNotNull(onError, "onError == null");
      AutoDisposeUtil.checkNotNull(onSubscribe, "onSubscribe == null");
      return new AutoDisposingCompletableObserver(lifecycle, action, onError, onSubscribe);
    }
  }
}
