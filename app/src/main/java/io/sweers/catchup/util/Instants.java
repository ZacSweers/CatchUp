package io.sweers.catchup.util;

import org.threeten.bp.Instant;
import org.threeten.bp.OffsetDateTime;

/**
 * Utilities for dealing with {@link Instant}
 */
public class Instants {

  private Instants() {
    // No instances
  }

  /**
   * Parses an instant from a time allowing for standard UTC (i.e. "Z") or UTC + offset.
   *
   * @param time the time string
   * @return an Instant representation of the time
   */
  public static Instant parsePossiblyOffsetInstant(String time) {
    if (!time.endsWith("Z")) {
      return OffsetDateTime.parse(time)
          .toInstant();
    } else {
      return Instant.parse(time);
    }
  }
}
