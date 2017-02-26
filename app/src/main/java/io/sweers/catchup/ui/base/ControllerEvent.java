package io.sweers.catchup.ui.base;

import io.reactivex.functions.Function;
import io.sweers.catchup.rx.autodispose.LifecycleEndedException;

/**
 * Controller lifecycle events
 */
public enum ControllerEvent {
  CREATE, CREATE_VIEW, ATTACH, DETACH, DESTROY_VIEW, DESTROY;

  @SuppressWarnings("UnnecessaryDefaultInEnumSwitch")
  // https://github.com/google/error-prone/issues/548
  static final Function<ControllerEvent, ControllerEvent> LIFECYCLE = lastEvent -> {
    switch (lastEvent) {
      case CREATE:
        return DESTROY;
      case CREATE_VIEW:
        return DESTROY_VIEW;
      case ATTACH:
        return DETACH;
      case DETACH:
        return DESTROY_VIEW;
      case DESTROY_VIEW:
        return DESTROY;
      case DESTROY:
        throw new LifecycleEndedException(
            "Cannot bind to Controller lifecycle after it's been destroyed.");
      default:
        throw new UnsupportedOperationException("Binding to " + lastEvent + " not yet implemented");
    }
  };
}
