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

package io.sweers.catchup.data.github.model

import com.google.auto.value.AutoValue
import org.threeten.bp.LocalDate

import org.threeten.bp.format.DateTimeFormatter.ISO_LOCAL_DATE

@AutoValue
abstract class SearchQuery {

  abstract fun createdSince(): LocalDate?

  abstract fun minStars(): Int

  override fun toString(): String {
    // Returning null here is not ideal, but it lets retrofit drop the query param altogether.
    val builder = StringBuilder()
    if (createdSince() != null) {
      builder.append("created:>=")
          .append(ISO_LOCAL_DATE.format(createdSince()!!))
          .append(' ')
    }
    if (minStars() != 0) {
      builder.append("stars:>=")
          .append(minStars())
    }
    return builder.toString()
        .trim { it <= ' ' }
  }

  @AutoValue.Builder
  abstract class Builder {
    abstract fun createdSince(createdSince: LocalDate): Builder

    abstract fun minStars(minStars: Int): Builder

    abstract fun build(): SearchQuery
  }

  companion object {
    fun builder(): Builder {
      return AutoValue_SearchQuery.Builder()
    }
  }
}
