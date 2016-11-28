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
}
