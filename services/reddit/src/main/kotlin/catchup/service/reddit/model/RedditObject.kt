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
package catchup.service.reddit.model

import androidx.annotation.Keep
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.datetime.Instant

@Keep @JsonClass(generateAdapter = false) sealed interface RedditObject

@Keep
interface RedditSubmission {
  val author: String
  @Json(name = "author_flair_text") val authorFlairText: String?
  @Json(name = "banned_by") val bannedBy: String?
  val created: Instant
  @Json(name = "created_utc") val createdUtc: Instant
  val gilded: Int
  val id: String
  val name: String
  val saved: Boolean
  val score: Int
  val subreddit: String
  val ups: Int
}

@Keep
@JsonClass(generateAdapter = true)
data class RedditComment(
  val body: String,
  @Json(name = "body_html") val bodyHtml: String,
  val controversiality: Int,
  val depth: Int,
  @Json(name = "link_id") val linkId: String,
  @Json(name = "parent_id") val parentId: String,
  /**
   * Ugh-asaurus
   *
   * @return list of comments. Or false. Because yeah.
   */
  val replies: RedditObject?,
  @Json(name = "subreddit_id") val subredditId: String,

  // Inherited from RedditSubmission. A little grody
  override val author: String,
  @Json(name = "author_flair_text") override val authorFlairText: String?,
  @Json(name = "banned_by") override val bannedBy: String?,
  override val created: Instant,
  @Json(name = "created_utc") override val createdUtc: Instant,
  override val gilded: Int,
  override val id: String,
  override val name: String,
  override val saved: Boolean,
  override val score: Int,
  override val subreddit: String,
  override val ups: Int,
) : RedditObject, RedditSubmission

@Keep
@JsonClass(generateAdapter = true)
data class RedditLink(
  val clicked: Boolean,
  val domain: String?,
  val hidden: Boolean,
  @Json(name = "is_self") val isSelf: Boolean,
  @Json(name = "link_flair_text") val linkFlairText: String?,
  @Json(name = "num_comments") val commentsCount: Int,
  val permalink: String,
  val selftext: String?,
  @Json(name = "selftext_html") val selftextHtml: String?,
  val stickied: Boolean,
  val thumbnail: String,
  val title: String,
  val url: String,
  val visited: Boolean,
  @Json(name = "post_hint") val postHint: String?,

  // Inherited from RedditSubmission. A little grody
  override val author: String,
  @Json(name = "author_flair_text") override val authorFlairText: String?,
  @Json(name = "banned_by") override val bannedBy: String?,
  override val created: Instant,
  @Json(name = "created_utc") override val createdUtc: Instant,
  override val gilded: Int,
  override val id: String,
  override val name: String,
  override val saved: Boolean,
  override val score: Int,
  override val subreddit: String,
  override val ups: Int,
) : RedditObject, RedditSubmission

@Keep
@JsonClass(generateAdapter = true)
data class RedditListing(
  val after: String? = null,
  val before: String? = null,
  val children: List<RedditObject>,
  val modhash: String,
) : RedditObject

/*
"count": 7624,
"name": "t1_djk44jk",
"id": "djk44jk",
"parent_id": "t3_6k7zjc",
"depth": 0,
"children": [
    "djk44jk",
    "djk68e9",
    "djk91jb",
    "djk8c97",
    "djk68er",
    "djk44k4",
    "djk8c9k",
    and hundreds more of these
]
 */
@Keep
@JsonClass(generateAdapter = true)
data class RedditMore(
  val count: Int,
  val name: String,
  val id: String,
  @Json(name = "parent_id") val parentId: String,
  val depth: Int,
  val children: List<String>,
) : RedditObject
