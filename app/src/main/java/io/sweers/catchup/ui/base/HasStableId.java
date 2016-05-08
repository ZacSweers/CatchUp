package io.sweers.catchup.ui.base;

/**
 * Helper interface for use in {@link BaseNewsController} subclasses to report IDs.
 */
public interface HasStableId {
  long stableId();
}
