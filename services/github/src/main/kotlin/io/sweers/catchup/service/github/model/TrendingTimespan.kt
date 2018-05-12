/*
 * Copyright (c) 2018 Zac Sweers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.sweers.catchup.service.github.model

import org.threeten.bp.LocalDate
import org.threeten.bp.temporal.ChronoUnit
import org.threeten.bp.temporal.TemporalUnit

// https://github.com/google/error-prone/issues/512
internal enum class TrendingTimespan constructor(private val contextualReference: String,
    duration: Int,
    private val durationUnit: TemporalUnit) {
  DAY("today", 1, ChronoUnit.DAYS),
  WEEK("last week", 1, ChronoUnit.WEEKS),
  MONTH("last month", 1, ChronoUnit.MONTHS);

  private val duration: Long = duration.toLong()

  /**
   * Returns a [LocalDate] to use with [SearchQuery.Builder.createdSince].
   */
  fun createdSince(): LocalDate {
    return LocalDate.now()
        .minus(duration, durationUnit)
  }

  override fun toString(): String = contextualReference
}
