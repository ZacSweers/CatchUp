package io.sweers.catchup.ui.base;

/**
 * Signifies an error occurred due to execution starting before the lifecycle has started.
 */
public class LifecycleNotStartedException extends OutsideLifecycleException {

  public LifecycleNotStartedException() {
    super("Lifecycle hasn't started!");
  }
}
