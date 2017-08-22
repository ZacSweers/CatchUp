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

package io.sweers.catchup.data.dribbble.model

import com.google.auto.value.AutoValue
import com.squareup.moshi.Json
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import org.threeten.bp.Instant
import java.util.Date

/**
 * Models a commend on a Dribbble shot.
 */
@AutoValue
abstract class Comment {

  abstract fun body(): String

  @Json(name = "created_at") abstract fun createdAt(): Instant

  abstract fun id(): Long

  @Json(name = "likes_count") abstract fun likesCount(): Long

  @Json(name = "likes_url") abstract fun likesUrl(): String

  @Json(name = "updated_at") abstract fun updatedAt(): Date

  abstract fun user(): User

  companion object {

    @JvmStatic
    fun jsonAdapter(moshi: Moshi): JsonAdapter<Comment> {
      return AutoValue_Comment.MoshiJsonAdapter(moshi)
    }
  }
}
