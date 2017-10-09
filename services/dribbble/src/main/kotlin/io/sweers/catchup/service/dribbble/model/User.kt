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
 * Models a dribbble user
 */
@AutoValue
internal abstract class User {

  @Json(name = "avatar_url") abstract fun avatarUrl(): String

  abstract fun bio(): String

  @Json(name = "buckets_count") abstract fun bucketsCount(): Int

  @Json(name = "buckets_url") abstract fun bucketsUrl(): String

  @Json(name = "created_at") abstract fun createdAt(): Instant

  @Json(name = "followers_count") abstract fun followersCount(): Int

  @Json(name = "followers_url") abstract fun followersUrl(): String

  @Json(name = "following_url") abstract fun followingUrl(): String

  @Json(name = "followings_count") abstract fun followingsCount(): Int

  @Json(name = "html_url") abstract fun htmlUrl(): String

  abstract fun id(): Long

  @Json(name = "likes_count") abstract fun likesCount(): Int

  @Json(name = "likes_url") abstract fun likesUrl(): String

  abstract fun links(): Map<String, String>

  abstract fun location(): String?

  abstract fun name(): String

  abstract fun pro(): Boolean?

  @Json(name = "projects_count") abstract fun projectsCount(): Int

  @Json(name = "projects_url") abstract fun projectsUrl(): String

  @Json(name = "shots_count") abstract fun shotsCount(): Int

  @Json(name = "shots_url") abstract fun shotsUrl(): String

  @Json(name = "teams_count") abstract fun teamsCount(): Int

  @Json(name = "teams_url") abstract fun teamsUrl(): String?

  abstract fun type(): String

  @Json(name = "updated_at") abstract fun updatedAt(): Instant

  abstract fun username(): String

  companion object {

    @JvmStatic
    fun jsonAdapter(moshi: Moshi): JsonAdapter<User> = AutoValue_User.MoshiJsonAdapter(moshi)
  }
}
