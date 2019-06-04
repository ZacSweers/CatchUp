/*
 * Copyright (C) 2019. Zac Sweers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.sweers.catchup.service.producthunt.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import io.sweers.catchup.util.e
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.threeten.bp.Instant

/**
 * Models a post on Product Hunt.
 */
@JsonClass(generateAdapter = true)
internal data class Post(
  @Json(name = "comments_count") val commentsCount: Int,
  @Json(name = "created_at") val createdAt: Instant,
  @Json(name = "discussion_url") val discussionUrl: String?,
  val id: Long,
  val makers: List<User>,
  @Json(name = "maker_inside") val makerInside: Boolean,
  val name: String,
  @Json(name = "redirect_url") val redirectUrl: String,
  @Json(name = "screenshot_url") val screenshotUrl: Map<String, String>,
  val tagline: String,
  val topics: List<Topic>? = null,
  val user: User,
  @Json(name = "votes_count") val votesCount: Int
) {

  val firstTopic: String?
    get() {
      val topics = topics
      if (topics != null && !topics.isEmpty()) {
        return topics[0].name
      }
      return null
    }

  val category: String?
    get() {
      val discussion = discussionUrl
      if (discussion != null) {
        return discussion.toHttpUrlOrNull()!!.pathSegments[0]
      }
      return null
    }

  fun getScreenshotUrl(width: Int): String? {
    var url: String? = null
    for (widthStr in screenshotUrl.keys) {
      url = screenshotUrl[widthStr]
      try {
        val screenshotWidth = Integer.parseInt(widthStr.substring(0, widthStr.length - 2))
        if (screenshotWidth > width) {
          break
        }
      } catch (nfe: NumberFormatException) {
        e(nfe) { "FailedGetScreenshotUrl" }
      }
    }

    return url
  }
}
