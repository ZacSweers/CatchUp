package io.sweers.catchup.rx.boundlifecycle.observers;

import android.support.annotation.CheckResult;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.sweers.catchup.rx.boundlifecycle.LifecycleProvider;
import io.sweers.catchup.rx.boundlifecycle.observers.BoundObserver.Creator;

public final class BoundObservers {

  @CheckResult
  public static <T> Creator<T> forObservable(Observable<?> lifecycle) {
    return new Creator<>(lifecycle);
  }

  @CheckResult
  public static <T> Creator<T> forObservable(LifecycleProvider<?> lifecycleProvider) {
    return new Creator<>(lifecycleProvider);
  }

  @CheckResult
  public static <T> Creator<T> forObservable(Maybe<?> lifecycle) {
    return new Creator<>(lifecycle);
  }

  @CheckResult
  public static <T> BoundSingleObserver.Creator<T> forSingle(Observable<?> lifecycle) {
    return new BoundSingleObserver.Creator<>(lifecycle);
  }

  @CheckResult
  public static <T> BoundSingleObserver.Creator<T> forSingle(LifecycleProvider<?> lifecycleProvider) {
    return new BoundSingleObserver.Creator<>(lifecycleProvider);
  }

  @CheckResult
  public static <T> BoundSingleObserver.Creator<T> forSingle(Maybe<?> lifecycle) {
    return new BoundSingleObserver.Creator<>(lifecycle);
  }

  @CheckResult
  public static <T> BoundMaybeObserver.Creator<T> forMaybe(Observable<?> lifecycle) {
    return new BoundMaybeObserver.Creator<>(lifecycle);
  }

  @CheckResult
  public static <T> BoundMaybeObserver.Creator<T> forMaybe(LifecycleProvider<?> lifecycleProvider) {
    return new BoundMaybeObserver.Creator<>(lifecycleProvider);
  }

  @CheckResult
  public static <T> BoundMaybeObserver.Creator<T> forMaybe(Maybe<?> lifecycle) {
    return new BoundMaybeObserver.Creator<>(lifecycle);
  }

  @CheckResult
  public static BoundCompletableObserver.Creator forCompletable(Observable<?> lifecycle) {
    return new BoundCompletableObserver.Creator(lifecycle);
  }

  @CheckResult
  public static BoundCompletableObserver.Creator forCompletable(LifecycleProvider<?> lifecycleProvider) {
    return new BoundCompletableObserver.Creator(lifecycleProvider);
  }

  @CheckResult
  public static BoundCompletableObserver.Creator forCompletable(Maybe<?> lifecycle) {
    return new BoundCompletableObserver.Creator(lifecycle);
  }
}
