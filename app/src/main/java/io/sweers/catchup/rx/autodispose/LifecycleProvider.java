package io.sweers.catchup.rx.autodispose;

import android.support.annotation.CheckResult;
import android.support.annotation.Nullable;
import io.reactivex.Observable;
import io.reactivex.functions.Function;

/**
 * @param <E> the the lifecycle event type.
 */
public interface LifecycleProvider<E> {

  /**
   * @return a sequence of lifecycle events.
   */
  @CheckResult
  Observable<E> lifecycle();

  /**
   * @return a sequence of lifecycle events.
   */
  @CheckResult
  Function<E, E> correspondingEvents();

  /**
   * @return the last seen lifecycle event, or {@code null} if none.
   */
  @Nullable
  E peekLifecycle();
}
