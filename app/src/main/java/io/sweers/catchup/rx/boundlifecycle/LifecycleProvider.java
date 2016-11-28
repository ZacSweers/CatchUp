package io.sweers.catchup.rx.boundlifecycle;

import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import io.reactivex.Observable;
import io.reactivex.functions.Function;
import javax.annotation.Nonnull;

public interface LifecycleProvider<E> {

  /**
   * @return a sequence of lifecycle events
   */
  @Nonnull
  @CheckResult
  Observable<E> lifecycle();

  /**
   * @return a sequence of lifecycle events
   */
  @Nonnull
  @CheckResult
  Function<E, E> correspondingEvents();

  boolean hasLifecycleStarted();

  static <E> Observable<Boolean> mapEvents(@NonNull Observable<E> lifecycle,
      @NonNull Function<E, E> correspondingEvents) {
    return Observable.combineLatest(
        lifecycle.take(1)
            .map(correspondingEvents),
        lifecycle.skip(1),
        (bindUntilEvent, lifecycleEvent) -> lifecycleEvent.equals(bindUntilEvent))
        .filter(b -> b)
        .take(1);
  }
}
