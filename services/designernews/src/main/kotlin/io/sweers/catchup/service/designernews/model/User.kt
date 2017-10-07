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

package io.sweers.catchup.service.designernews.model

import com.google.auto.value.AutoValue
import com.squareup.moshi.Json
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi

/**
 * Models a Designer News User
 */
@AutoValue
internal abstract class User {

  @Json(name = "cover_photo_url") abstract fun coverPhotoUrl(): String

  @Json(name = "display_name") abstract fun displayName(): String

  @Json(name = "first_name") abstract fun firstName(): String

  abstract fun id(): Long

  abstract fun job(): String

  @Json(name = "last_name") abstract fun lastName(): String

  @Json(name = "portrait_url") abstract fun portraitUrl(): String

  companion object {

    @JvmStatic
    fun jsonAdapter(moshi: Moshi): JsonAdapter<User> = AutoValue_User.MoshiJsonAdapter(moshi)

    val NONE: User = AutoValue_User(
        "",
        "",
        "",
        0,
        "",
        "",
        ""
    )
  }
}
