package io.sweers.catchup.ui.base;

import io.reactivex.functions.Function;

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
      default:
        throw new OutsideLifecycleException(
            "Cannot bind to Controller lifecycle when outside of it.");
    }
  };
}
