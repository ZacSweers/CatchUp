package io.sweers.catchup.ui.base;

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
        throw new OutsideLifecycleException("Cannot bind to Activity lifecycle when outside of it.");
      default:
        throw new UnsupportedOperationException("Binding to " + lastEvent + " not yet implemented");
    }
  };
}
