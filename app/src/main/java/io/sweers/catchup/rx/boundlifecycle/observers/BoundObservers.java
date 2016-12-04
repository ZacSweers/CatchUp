package io.sweers.catchup.rx.boundlifecycle.observers;

import android.support.annotation.NonNull;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.sweers.catchup.rx.boundlifecycle.LifecycleProvider;
import io.sweers.catchup.rx.boundlifecycle.observers.BoundObserver.Creator;

public final class BoundObservers {

  public static <T> Creator<T> forObservable(@NonNull Observable<?> lifecycle) {
    return new Creator<>(lifecycle);
  }

  public static <T> Creator<T> forObservable(
      @NonNull LifecycleProvider<?> lifecycleProvider) {
    return new Creator<>(lifecycleProvider);
  }

  public static <T> Creator<T> forObservable(@NonNull Maybe<?> lifecycle) {
    return new Creator<>(lifecycle);
  }

  public static <T> BoundSingleObserver.Creator<T> forSingle(@NonNull Observable<?> lifecycle) {
    return new BoundSingleObserver.Creator<>(lifecycle);
  }

  public static <T> BoundSingleObserver.Creator<T> forSingle(
      @NonNull LifecycleProvider<?> lifecycleProvider) {
    return new BoundSingleObserver.Creator<>(lifecycleProvider);
  }

  public static <T> BoundSingleObserver.Creator<T> forSingle(@NonNull Maybe<?> lifecycle) {
    return new BoundSingleObserver.Creator<>(lifecycle);
  }

  public static <T> BoundMaybeObserver.Creator<T> forMaybe(@NonNull Observable<?> lifecycle) {
    return new BoundMaybeObserver.Creator<>(lifecycle);
  }

  public static <T> BoundMaybeObserver.Creator<T> forMaybe(
      @NonNull LifecycleProvider<?> lifecycleProvider) {
    return new BoundMaybeObserver.Creator<>(lifecycleProvider);
  }

  public static <T> BoundMaybeObserver.Creator<T> forMaybe(@NonNull Maybe<?> lifecycle) {
    return new BoundMaybeObserver.Creator<>(lifecycle);
  }

  public static BoundCompletableObserver.Creator forCompletable(@NonNull Observable<?> lifecycle) {
    return new BoundCompletableObserver.Creator(lifecycle);
  }

  public static BoundCompletableObserver.Creator forCompletable(
      @NonNull LifecycleProvider<?> lifecycleProvider) {
    return new BoundCompletableObserver.Creator(lifecycleProvider);
  }

  public static BoundCompletableObserver.Creator forCompletable(@NonNull Maybe<?> lifecycle) {
    return new BoundCompletableObserver.Creator(lifecycle);
  }
}
