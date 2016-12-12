package io.sweers.catchup.ui.base;

import io.reactivex.functions.Function;
import io.sweers.catchup.rx.autodispose.LifecycleEndedException;

/**
 * Controller lifecycle events
 */
public enum ControllerEvent {
  CREATE, ATTACH, CREATE_VIEW, DESTROY_VIEW, DETACH, DESTROY;
  static final Function<ControllerEvent, ControllerEvent> LIFECYCLE = lastEvent -> {
    switch (lastEvent) {
      case CREATE:
        return DESTROY;
      case ATTACH:
        return DETACH;
      case CREATE_VIEW:
        return DESTROY_VIEW;
      case DETACH:
        return DESTROY;
      case DESTROY:
        throw new LifecycleEndedException(
            "Cannot bind to Controller lifecycle after it's been destroyed.");
      default:
        throw new UnsupportedOperationException("Binding to " + lastEvent + " not yet implemented");
    }
  };
}
