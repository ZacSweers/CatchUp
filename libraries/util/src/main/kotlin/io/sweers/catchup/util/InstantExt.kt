/*
 * Copyright (C) 2019. Zac Sweers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@file:Suppress("NOTHING_TO_INLINE")

package io.sweers.catchup.util

import java.time.Instant
import java.time.OffsetDateTime

/*
 * Utilities for dealing with [Instant]
 */

/**
 * Parses an instant from a time allowing for standard UTC (i.e. "Z") or UTC + offset.
 *
 * @return an Instant representation of the time
 */
inline fun String.parsePossiblyOffsetInstant(): Instant {
  return if (!endsWith("Z")) {
    OffsetDateTime.parse(this)
        .toInstant()
  } else {
    Instant.parse(this)
  }
}
