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
import io.sweers.catchup.ui.base.HasStableId
import okhttp3.HttpUrl
import org.threeten.bp.Instant
import timber.log.Timber

/**
 * Models a post on Product Hunt.
 */
@AutoValue
abstract class Post : HasStableId {

  abstract fun comments_count(): Int

  abstract fun created_at(): Instant

  abstract fun discussion_url(): String?

  abstract fun id(): Long

  abstract fun makers(): List<User>

  abstract fun maker_inside(): Boolean

  abstract fun name(): String

  abstract fun redirect_url(): String

  abstract fun screenshot_url(): Map<String, String>

  abstract fun tagline(): String

  abstract fun topics(): List<Topic>?

  abstract fun user(): User

  abstract fun votes_count(): Int

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
      val discussion = discussion_url()
      if (discussion != null) {
        return HttpUrl.parse(discussion)!!.pathSegments()[0]
      }
      return null
    }

  fun getScreenshotUrl(width: Int): String? {
    var url: String? = null
    for (widthStr in screenshot_url().keys) {
      url = screenshot_url()[widthStr]
      try {
        val screenshotWidth = Integer.parseInt(widthStr.substring(0, widthStr.length - 2))
        if (screenshotWidth > width) {
          break
        }
      } catch (nfe: NumberFormatException) {
        Timber.e(nfe, "FailedGetScreenshotUrl")
      }

    }

    return url
  }

  override fun stableId(): Long = id()

  @AutoValue.Builder
  interface Builder {
    fun comments_count(count: Int): Builder

    fun created_at(date: Instant): Builder

    fun discussion_url(url: String): Builder

    fun id(id: Long): Builder

    fun makers(makers: List<@JvmSuppressWildcards User>): Builder

    fun maker_inside(makerInside: Boolean): Builder

    fun name(name: String): Builder

    fun redirect_url(url: String): Builder

    fun screenshot_url(url: Map<String, String>): Builder

    fun tagline(tagline: String): Builder

    fun topics(topics: List<@JvmSuppressWildcards Topic>?): Builder

    fun user(user: User): Builder

    fun votes_count(count: Int): Builder

    fun build(): Post
  }

  companion object {

    @JvmStatic
    fun jsonAdapter(moshi: Moshi): JsonAdapter<Post> {
      return AutoValue_Post.MoshiJsonAdapter(moshi)
    }

    fun builder(): Builder {
      // Ew
      return `$AutoValue_Post`.Builder()
    }
  }
}
