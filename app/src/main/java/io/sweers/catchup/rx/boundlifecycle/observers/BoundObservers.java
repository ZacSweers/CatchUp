package io.sweers.catchup.rx.boundlifecycle.observers;

import android.support.annotation.NonNull;
import io.reactivex.Observable;
import io.sweers.catchup.rx.boundlifecycle.LifecycleProvider;

public final class BoundObservers {

  public static <T> BoundObserver.BoundObserverCreator<T> forObservable(
      @NonNull Observable<?> lifecycle) {
    return new BoundObserver.BoundObserverCreator<>(lifecycle);
  }

  public static <T> BoundObserver.BoundObserverCreator<T> forObservable(
      @NonNull LifecycleProvider<?> lifecycleProvider) {
    return new BoundObserver.BoundObserverCreator<>(lifecycleProvider);
  }

  public static <T> BoundSingleObserver.BoundSingleObserverCreator<T> forSingle(
      @NonNull Observable<?> lifecycle) {
    return new BoundSingleObserver.BoundSingleObserverCreator<>(lifecycle);
  }

  public static <T> BoundSingleObserver.BoundSingleObserverCreator<T> forSingle(
      @NonNull LifecycleProvider<?> lifecycleProvider) {
    return new BoundSingleObserver.BoundSingleObserverCreator<>(lifecycleProvider);
  }

  public static <T> BoundMaybeObserver.BoundMaybeObserverCreator<T> forMaybe(
      @NonNull Observable<?> lifecycle) {
    return new BoundMaybeObserver.BoundMaybeObserverCreator<>(lifecycle);
  }

  public static <T> BoundMaybeObserver.BoundMaybeObserverCreator<T> forMaybe(
      @NonNull LifecycleProvider<?> lifecycleProvider) {
    return new BoundMaybeObserver.BoundMaybeObserverCreator<>(lifecycleProvider);
  }

  public static BoundCompletableObserver.BoundCompletableObserverCreator forCompletable(
      @NonNull Observable<?> lifecycle) {
    return new BoundCompletableObserver.BoundCompletableObserverCreator(lifecycle);
  }

  public static BoundCompletableObserver.BoundCompletableObserverCreator forCompletable(
      @NonNull LifecycleProvider<?> lifecycleProvider) {
    return new BoundCompletableObserver.BoundCompletableObserverCreator(lifecycleProvider);
  }
}
