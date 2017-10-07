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
import org.threeten.bp.Instant

/**
 * Models a Designer News story
 */
@AutoValue
internal abstract class Story {

  abstract fun badge(): String?

  abstract fun comment(): String?

  @Json(name = "comment_count") abstract fun commentCount(): Int

  @Json(name = "comment_html") abstract fun commentHtml(): String?

  @Json(name = "created_at") abstract fun createdAt(): Instant

  abstract fun hostname(): String?

  abstract fun id(): String

  abstract fun href(): String

  abstract fun title(): String

  abstract fun url(): String?

  abstract fun links(): Links

  @Json(name = "vote_count") abstract fun voteCount(): Int

  @Json(name = "twitter_handles") abstract fun twitterHandles(): List<String>

  companion object {

    @JvmStatic
    fun jsonAdapter(moshi: Moshi): JsonAdapter<Story> = AutoValue_Story.MoshiJsonAdapter(moshi)
  }
}
