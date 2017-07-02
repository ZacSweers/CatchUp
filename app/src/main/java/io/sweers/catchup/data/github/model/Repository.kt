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
import com.squareup.moshi.Json
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import org.threeten.bp.Instant

@AutoValue
abstract class Repository {

  @Json(name = "created_at") abstract fun createdAt(): Instant

  @Json(name = "full_name") abstract fun fullName(): String

  @Json(name = "html_url") abstract fun htmlUrl(): String

  abstract fun id(): Long

  abstract fun language(): String?

  abstract fun name(): String

  abstract fun owner(): User

  @Json(name = "stargazers_count") abstract fun starsCount(): Int

  @AutoValue.Builder
  interface Builder {
    fun createdAt(createdAt: Instant): Builder

    fun fullName(fullName: String): Builder

    fun htmlUrl(htmlUrl: String): Builder

    fun id(id: Long): Builder

    fun language(language: String?): Builder

    fun name(name: String): Builder

    fun owner(owner: User): Builder

    fun starsCount(starsCount: Int): Builder

    fun build(): Repository
  }

  companion object {

    @JvmStatic
    fun jsonAdapter(moshi: Moshi): JsonAdapter<Repository> {
      return AutoValue_Repository.MoshiJsonAdapter(moshi)
    }

    fun builder(): Builder {
      // ew
      return `$AutoValue_Repository`.Builder()
    }
  }
}
