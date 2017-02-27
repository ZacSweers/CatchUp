package io.sweers.catchup.ui.base;

import com.uber.autodispose.LifecycleEndedException;
import io.reactivex.functions.Function;

/**
 * Activity lifecycle events.
 */
public enum ActivityEvent {
  CREATE, START, RESUME, PAUSE, STOP, DESTROY;
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
    }
    throw new UnsupportedOperationException("Binding to " + lastEvent + " not yet implemented");
  };
}
