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
 * Models a comment on a designer news story.
 */
@JsonClass(generateAdapter = true)
internal data class Comment(
  val body: String,
  @Json(name = "body_html") val bodyHtml: String,
  val comments: List<Comment>,
  @Json(name = "created_at") val createdAt: Instant,
  val depth: Int,
  val id: Long,
  val upvoted: Boolean,
  @Json(name = "user_display_name") val userDisplayName: String,
  @Json(name = "user_id") val userId: Long,
  @Json(name = "user_job") val userJob: String?,
  @Json(name = "user_portrait_url") val userPortraitUrl: String?,
  @Json(name = "vote_count") val voteCount: Int
)
