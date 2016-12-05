package io.sweers.catchup.rx.boundlifecycle;

import android.support.annotation.CheckResult;
import io.reactivex.Observable;
import io.reactivex.functions.Function;

public interface LifecycleProvider<E> {

  /**
   * @return a sequence of lifecycle events
   */
  @CheckResult
  Observable<E> lifecycle();

  /**
   * @return a sequence of lifecycle events
   */
  @CheckResult
  Function<E, E> correspondingEvents();

  boolean hasLifecycleStarted();
}
