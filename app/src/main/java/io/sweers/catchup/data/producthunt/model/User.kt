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

package io.sweers.catchup.data.producthunt.model

import com.google.auto.value.AutoValue
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi

/**
 * Models a user on Product Hunt.
 */
@AutoValue
abstract class User {

  abstract fun created_at(): String

  abstract fun headline(): String?

  abstract fun id(): Long

  abstract fun image_url(): Map<String, String>

  abstract fun name(): String

  abstract fun profile_url(): String

  abstract fun username(): String

  abstract fun website_url(): String?

  @AutoValue.Builder
  interface Builder {
    fun created_at(created_at: String): Builder

    fun headline(headline: String?): Builder

    fun id(id: Long): Builder

    fun image_url(imageUrl: Map<String, String>): Builder

    fun name(name: String): Builder

    fun profile_url(profile_url: String): Builder

    fun username(username: String): Builder

    fun website_url(website_url: String?): Builder

    fun builder(): User
  }

  companion object {

    @JvmStatic
    fun jsonAdapter(moshi: Moshi): JsonAdapter<User> {
      return AutoValue_User.MoshiJsonAdapter(moshi)
    }

    fun builder(): Builder {
      // Ew
      return `$AutoValue_User`.Builder()
    }
  }
}
