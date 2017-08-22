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

package io.sweers.catchup.data.designernews.model

import com.google.auto.value.AutoValue
import com.squareup.moshi.Json
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import org.threeten.bp.Instant

/**
 * Models a comment on a designer news story.
 */
@AutoValue
abstract class Comment {

  abstract fun body(): String

  @Json(name = "body_html") abstract fun bodyHtml(): String

  abstract fun comments(): List<Comment>

  @Json(name = "created_at") abstract fun createdAt(): Instant

  abstract fun depth(): Int

  abstract fun id(): Long

  abstract fun upvoted(): Boolean

  @Json(name = "user_display_name") abstract fun userDisplayName(): String

  @Json(name = "user_id") abstract fun userId(): Long

  @Json(name = "user_job") abstract fun userJob(): String?

  @Json(name = "user_portrait_url") abstract fun userPortraitUrl(): String?

  @Json(name = "vote_count") abstract fun voteCount(): Int

  companion object {

    @JvmStatic
    fun jsonAdapter(moshi: Moshi): JsonAdapter<Comment> {
      return AutoValue_Comment.MoshiJsonAdapter(moshi)
    }
  }
}
