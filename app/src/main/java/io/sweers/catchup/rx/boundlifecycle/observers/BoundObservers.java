package io.sweers.catchup.rx.boundlifecycle.observers;

import android.support.annotation.NonNull;
import io.reactivex.Observable;
import io.sweers.catchup.rx.boundlifecycle.LifecycleProvider;
import io.sweers.catchup.rx.boundlifecycle.observers.BoundCompletableObserver.BoundCompletableObserverCreator;
import io.sweers.catchup.rx.boundlifecycle.observers.BoundMaybeObserver.BoundMaybeObserverCreator;
import io.sweers.catchup.rx.boundlifecycle.observers.BoundObserver.BoundObserverCreator;
import io.sweers.catchup.rx.boundlifecycle.observers.BoundSingleObserver.BoundSingleObserverCreator;

public final class BoundObservers {

  public static <T> BoundObserverCreator<T> forObservable(@NonNull Observable<?> lifecycle) {
    return new BoundObserverCreator<>(lifecycle);
  }

  public static <T> BoundObserverCreator<T> forObservable(
      @NonNull LifecycleProvider<?> lifecycleProvider) {
    return new BoundObserverCreator<>(lifecycleProvider);
  }

  public static <T> BoundSingleObserverCreator<T> forSingle(@NonNull Observable<?> lifecycle) {
    return new BoundSingleObserverCreator<>(lifecycle);
  }

  public static <T> BoundSingleObserverCreator<T> forSingle(
      @NonNull LifecycleProvider<?> lifecycleProvider) {
    return new BoundSingleObserverCreator<>(lifecycleProvider);
  }

  public static <T> BoundMaybeObserverCreator<T> forMaybe(@NonNull Observable<?> lifecycle) {
    return new BoundMaybeObserverCreator<>(lifecycle);
  }

  public static <T> BoundMaybeObserverCreator<T> forMaybe(
      @NonNull LifecycleProvider<?> lifecycleProvider) {
    return new BoundMaybeObserverCreator<>(lifecycleProvider);
  }

  public static BoundCompletableObserverCreator forCompletable(@NonNull Observable<?> lifecycle) {
    return new BoundCompletableObserverCreator(lifecycle);
  }

  public static BoundCompletableObserverCreator forCompletable(
      @NonNull LifecycleProvider<?> lifecycleProvider) {
    return new BoundCompletableObserverCreator(lifecycleProvider);
  }
}
