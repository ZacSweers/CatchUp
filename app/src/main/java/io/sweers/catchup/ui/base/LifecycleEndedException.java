package io.sweers.catchup.ui.base;

/**
 * Signifies an error occurred due to execution starting after the lifecycle has ended.
 */
public class LifecycleEndedException extends OutsideLifecycleException {

  public LifecycleEndedException(String s) {
    super(s);
  }
}
