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

package io.sweers.catchup.service.reddit.model

import com.google.auto.value.AutoValue
import com.squareup.moshi.Json
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import io.sweers.catchup.service.api.HasStableId
import org.threeten.bp.Instant

internal sealed class RedditObject

internal interface RedditSubmission : HasStableId {

  fun author(): String

  @Json(name = "author_flair_text")
  fun authorFlairText(): String?

  @Json(name = "banned_by")
  fun bannedBy(): String?

  fun created(): Instant

  @Json(name = "created_utc")
  fun createdUtc(): Instant

  fun gilded(): Int

  fun id(): String

  fun name(): String

  fun saved(): Boolean

  fun score(): Int

  fun subreddit(): String

  fun ups(): Int

  override fun stableId(): Long = id().hashCode().toLong()
}

@AutoValue
internal abstract class RedditComment : RedditObject(), RedditSubmission {

  abstract fun body(): String

  @Json(name = "body_html") abstract fun bodyHtml(): String

  abstract fun controversiality(): Int

  abstract fun depth(): Int

  @Json(name = "link_id") abstract fun linkId(): String

  @Json(name = "parent_id") abstract fun parentId(): String

  /**
   * Ugh-asaurus
   *
   * @return list of comments. Or false. Because yeah.
   */
  abstract fun replies(): RedditObject

  @Json(name = "subreddit_id") abstract fun subredditId(): String

  companion object {
    @JvmStatic
    fun jsonAdapter(moshi: Moshi): JsonAdapter<RedditComment> {
      return AutoValue_RedditComment.MoshiJsonAdapter(moshi)
    }
  }
}

@AutoValue
internal abstract class RedditLink : RedditObject(), RedditSubmission {

  abstract fun clicked(): Boolean

  abstract fun domain(): String?

  abstract fun hidden(): Boolean

  @Json(name = "is_self") abstract val isSelf: Boolean

  @Json(name = "link_flair_text") abstract fun linkFlairText(): String?

  @Json(name = "num_comments") abstract fun commentsCount(): Int

  abstract fun permalink(): String

  abstract fun selftext(): String?

  @Json(name = "selftext_html") abstract fun selftextHtml(): String?

  abstract fun stickied(): Boolean

  abstract fun thumbnail(): String

  abstract fun title(): String

  abstract fun url(): String

  abstract fun visited(): Boolean

  companion object {
    @JvmStatic
    fun jsonAdapter(moshi: Moshi): JsonAdapter<RedditLink> {
      return AutoValue_RedditLink.MoshiJsonAdapter(moshi)
    }
  }
}

@AutoValue
internal abstract class RedditListing : RedditObject() {

  abstract fun after(): String

  abstract fun before(): String?

  abstract fun children(): List<RedditObject>

  abstract fun modhash(): String

  companion object {
    @JvmStatic
    fun jsonAdapter(moshi: Moshi): JsonAdapter<RedditListing> {
      return AutoValue_RedditListing.MoshiJsonAdapter(moshi)
    }
  }
}
