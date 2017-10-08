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

package io.sweers.catchup.service.producthunt.model

import com.google.auto.value.AutoValue
import com.squareup.moshi.Json
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi

/**
 * Models a user on Product Hunt.
 */
@AutoValue
internal abstract class User {

  @Json(name = "created_at") abstract fun createdAt(): String

  abstract fun headline(): String?

  abstract fun id(): Long

  @Json(name = "image_url") abstract fun imageUrl(): Map<String, String>

  abstract fun name(): String

  @Json(name = "profile_url") abstract fun profileUrl(): String

  abstract fun username(): String

  @Json(name = "website_url") abstract fun websiteUrl(): String?

  @AutoValue.Builder
  interface Builder {
    fun createdAt(created_at: String): Builder

    fun headline(headline: String?): Builder

    fun id(id: Long): Builder

    fun imageUrl(imageUrl: Map<String, String>): Builder

    fun name(name: String): Builder

    fun profileUrl(profile_url: String): Builder

    fun username(username: String): Builder

    fun websiteUrl(website_url: String?): Builder

    fun builder(): User
  }

  companion object {

    @JvmStatic
    fun jsonAdapter(moshi: Moshi): JsonAdapter<User> = AutoValue_User.MoshiJsonAdapter(moshi)

    // Ew
    fun builder(): Builder = `$AutoValue_User`.Builder()
  }
}
