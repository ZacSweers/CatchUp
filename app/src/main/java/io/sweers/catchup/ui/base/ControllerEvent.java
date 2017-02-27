package io.sweers.catchup.ui.base;

import com.uber.autodispose.LifecycleEndedException;
import io.reactivex.functions.Function;

/**
 * Controller lifecycle events
 */
public enum ControllerEvent {
  CREATE, CREATE_VIEW, ATTACH, DETACH, DESTROY_VIEW, DESTROY;

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
    }
    throw new UnsupportedOperationException("Binding to " + lastEvent + " not yet implemented");
  };
}
