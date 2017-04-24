/*
 * Copyright (c) 2017 Zac Sweers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.sweers.catchup.util;

import android.support.annotation.NonNull;

import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public final class NumberUtil {

  private static final NavigableMap<Long, String> SUFFIXES = new TreeMap<Long, String>() {{
    put(1_000L, "k");
    put(1_000_000L, "M");
    put(1_000_000_000L, "G");
    put(1_000_000_000_000L, "T");
    put(1_000_000_000_000_000L, "P");
    put(1_000_000_000_000_000_000L, "E");
  }};

  private NumberUtil() {
    throw new InstantiationError();
  }

  @NonNull
  public static String format(long value) {
    String shortened = shorten(value);
    if (!shortened.isEmpty()
        && !Character.isDigit(shortened.substring(shortened.length() - 1).charAt(0))) {
      shortened = shortened.replace('.',
          new DecimalFormatSymbols(Locale.getDefault()).getDecimalSeparator());
    }
    return shortened;
  }

  @NonNull
  public static String shorten(long value) {
    //Long.MIN_VALUE == -Long.MIN_VALUE so we need an adjustment here
    if (value == Long.MIN_VALUE) {
      return shorten(Long.MIN_VALUE + 1);
    }
    if (value < 0) {
      return "-" + shorten(-value);
    }
    if (value < 1000) {
      return Long.toString(value); //deal with easy case
    }

    Map.Entry<Long, String> e = SUFFIXES.floorEntry(value);
    Long divideBy = e.getKey();
    String suffix = e.getValue();

    long truncated = value / (divideBy / 10); //the number part of the output times 10
    boolean hasDecimal = truncated < 100 && (truncated / 10d) != (truncated / 10);
    return hasDecimal ? (truncated / 10d) + suffix : (truncated / 10) + suffix;
  }
}
