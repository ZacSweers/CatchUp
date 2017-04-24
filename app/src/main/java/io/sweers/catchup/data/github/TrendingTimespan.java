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

package io.sweers.catchup.data.github;

import io.sweers.catchup.data.github.model.SearchQuery;
import org.threeten.bp.LocalDate;
import org.threeten.bp.temporal.ChronoUnit;
import org.threeten.bp.temporal.TemporalUnit;

public enum TrendingTimespan {
  DAY("today", 1, ChronoUnit.DAYS), WEEK("last week", 1, ChronoUnit.WEEKS), MONTH(
      "last month",
      1,
      ChronoUnit.MONTHS);

  private final String name;
  private final long duration;

  @SuppressWarnings("ImmutableEnumChecker") // https://github.com/google/error-prone/issues/512
  private final TemporalUnit durationUnit;

  TrendingTimespan(String name, int duration, TemporalUnit durationUnit) {
    this.name = name;
    this.duration = duration;
    this.durationUnit = durationUnit;
  }

  /**
   * Returns a {@code LocalDate} to use with {@link SearchQuery.Builder#createdSince(LocalDate)}.
   */
  public LocalDate createdSince() {
    return LocalDate.now()
        .minus(duration, durationUnit);
  }

  @Override public String toString() {
    return name;
  }
}
