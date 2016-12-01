package io.sweers.catchup.ui.base;

/**
 * Signifies an error occurred due to execution starting outside the lifecycle.
 */
public class OutsideLifecycleException extends RuntimeException {

  public OutsideLifecycleException(String s) {
    super(s);
  }
}
