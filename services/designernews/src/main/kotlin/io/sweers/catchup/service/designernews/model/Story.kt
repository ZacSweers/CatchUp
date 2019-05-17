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
package io.sweers.catchup.service.designernews.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.threeten.bp.Instant

/**
 * Models a Designer News story
 */
@JsonClass(generateAdapter = true)
internal data class Story(
  val badge: String?,
  val comment: String?,
  @Json(name = "comment_count") val commentCount: Int,
  @Json(name = "comment_html") val commentHtml: String?,
  @Json(name = "created_at") val createdAt: Instant,
  val hostname: String?,
  val id: String,
  val href: String,
  val title: String,
  val url: String?,
  val links: Links,
  @Json(name = "vote_count") val voteCount: Int,
  @Json(name = "twitter_handles") val twitterHandles: List<String>
)
