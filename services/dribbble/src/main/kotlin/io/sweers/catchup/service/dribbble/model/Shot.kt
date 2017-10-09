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

package io.sweers.catchup.service.dribbble.model

import com.google.auto.value.AutoValue
import com.squareup.moshi.Json
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import org.threeten.bp.Instant

/**
 * Models a dibbble shot
 */
@AutoValue
internal abstract class Shot {

  abstract fun animated(): Boolean

  @Json(name = "attachments_count")
  abstract fun attachmentsCount(): Long

  @Json(name = "attachments_url")
  abstract fun attachmentsUrl(): String

  @Json(name = "buckets_count")
  abstract fun bucketsCount(): Long

  @Json(name = "buckets_url")
  abstract fun bucketsUrl(): String

  @Json(name = "comments_count")
  abstract fun commentsCount(): Long

  @Json(name = "comments_url")
  abstract fun commentsUrl(): String

  @Json(name = "created_at")
  abstract fun createdAt(): Instant

  abstract fun description(): String?

  abstract fun height(): Long

  @Json(name = "html_url")
  abstract fun htmlUrl(): String

  abstract fun id(): Long

  abstract fun images(): Images

  @Json(name = "likes_count")
  abstract fun likesCount(): Long

  @Json(name = "likes_url")
  abstract fun likesUrl(): String

  @Json(name = "projects_url")
  abstract fun projectsUrl(): String

  @Json(name = "rebounds_count")
  abstract fun reboundsCount(): Long

  @Json(name = "rebounds_url")
  abstract fun reboundsUrl(): String

  abstract fun tags(): List<String>

  abstract fun team(): Team?

  @Json(name = "updated_at")
  abstract fun updatedAt(): Instant

  abstract fun user(): User

  @Json(name = "views_count")
  abstract fun viewsCount(): Long

  abstract fun width(): Long

  companion object {

    @JvmStatic
    fun jsonAdapter(moshi: Moshi): JsonAdapter<Shot> = AutoValue_Shot.MoshiJsonAdapter(moshi)
  }
}
