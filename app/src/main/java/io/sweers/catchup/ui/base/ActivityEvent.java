package io.sweers.catchup.ui.base;

import io.reactivex.functions.Function;
import io.sweers.catchup.rx.autodispose.LifecycleEndedException;

/**
 * Activity lifecycle events.
 */
public enum ActivityEvent {
  CREATE, START, RESUME, PAUSE, STOP, DESTROY;
  @SuppressWarnings("UnnecessaryDefaultInEnumSwitch")
  // https://github.com/google/error-prone/issues/548
  static final Function<ActivityEvent, ActivityEvent> LIFECYCLE = lastEvent -> {
    switch (lastEvent) {
      case CREATE:
        return DESTROY;
      case START:
        return STOP;
      case RESUME:
        return PAUSE;
      case PAUSE:
        return STOP;
      case STOP:
        return DESTROY;
      case DESTROY:
        throw new LifecycleEndedException(
            "Cannot bind to Activity lifecycle after it's been destroyed.");
      default:
        throw new UnsupportedOperationException("Binding to " + lastEvent + " not yet implemented");
    }
  };
}
