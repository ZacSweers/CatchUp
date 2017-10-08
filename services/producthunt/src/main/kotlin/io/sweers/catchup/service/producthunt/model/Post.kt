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
//import io.sweers.catchup.util.e
import okhttp3.HttpUrl
import org.threeten.bp.Instant

/**
 * Models a post on Product Hunt.
 */
@AutoValue
internal abstract class Post {

  @Json(name = "comments_count") abstract fun commentsCount(): Int

  @Json(name = "created_at") abstract fun createdAt(): Instant

  @Json(name = "discussion_url") abstract fun discussionUrl(): String?

  abstract fun id(): Long

  abstract fun makers(): List<User>

  @Json(name = "maker_inside") abstract fun makerInside(): Boolean

  abstract fun name(): String

  @Json(name = "redirect_url") abstract fun redirectUrl(): String

  @Json(name = "screenshot_url") abstract fun screenshotUrl(): Map<String, String>

  abstract fun tagline(): String

  abstract fun topics(): List<Topic>?

  abstract fun user(): User

  @Json(name = "votes_count") abstract fun votesCount(): Int

  val firstTopic: String?
    get() {
      val topics = topics()
      if (topics != null && !topics.isEmpty()) {
        return topics[0].name()
      }
      return null
    }

  val category: String?
    get() {
      val discussion = discussionUrl()
      if (discussion != null) {
        return HttpUrl.parse(discussion)!!.pathSegments()[0]
      }
      return null
    }

  fun getScreenshotUrl(width: Int): String? {
    var url: String? = null
    for (widthStr in screenshotUrl().keys) {
      url = screenshotUrl()[widthStr]
      try {
        val screenshotWidth = Integer.parseInt(widthStr.substring(0, widthStr.length - 2))
        if (screenshotWidth > width) {
          break
        }
      } catch (nfe: NumberFormatException) {
//        e(nfe) { "FailedGetScreenshotUrl" }
      }

    }

    return url
  }

  @AutoValue.Builder
  interface Builder {
    fun commentsCount(count: Int): Builder

    fun createdAt(date: Instant): Builder

    fun discussionUrl(url: String): Builder

    fun id(id: Long): Builder

    fun makers(makers: List<@JvmSuppressWildcards User>): Builder

    fun makerInside(makerInside: Boolean): Builder

    fun name(name: String): Builder

    fun redirectUrl(url: String): Builder

    fun screenshotUrl(url: Map<String, String>): Builder

    fun tagline(tagline: String): Builder

    fun topics(topics: List<@JvmSuppressWildcards Topic>?): Builder

    fun user(user: User): Builder

    fun votesCount(count: Int): Builder

    fun build(): Post
  }

  companion object {

    @JvmStatic
    fun jsonAdapter(moshi: Moshi): JsonAdapter<Post> = AutoValue_Post.MoshiJsonAdapter(moshi)

    // Ew
    fun builder(): Builder = `$AutoValue_Post`.Builder()
  }
}
