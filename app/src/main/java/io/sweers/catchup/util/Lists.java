package io.sweers.catchup.util;

import android.support.annotation.Nullable;
import java.util.Collections;
import java.util.List;

/**
 * List utilities
 */
public final class Lists {

  public static <T> List<T> emptyIfNull(@Nullable List<T> input) {
    if (input == null) {
      return Collections.emptyList();
    } else {
      return input;
    }
  }
}
