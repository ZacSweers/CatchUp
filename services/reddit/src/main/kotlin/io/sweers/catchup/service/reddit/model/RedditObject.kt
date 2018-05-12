/*
 * Copyright (c) 2018 Zac Sweers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.sweers.catchup.service.reddit.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import io.sweers.catchup.service.api.HasStableId
import org.threeten.bp.Instant

internal sealed class RedditObject

internal interface RedditSubmission : HasStableId {
  val author: String
  @Json(name = "author_flair_text")
  val authorFlairText: String?
  @Json(name = "banned_by")
  val bannedBy: String?
  val created: Instant
  @Json(name = "created_utc")
  val createdUtc: Instant
  val gilded: Int
  val id: String
  val name: String
  val saved: Boolean
  val score: Int
  val subreddit: String
  val ups: Int
  override fun stableId(): Long = id.hashCode().toLong()
}

@JsonClass(generateAdapter = true)
internal data class RedditComment(
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
    val replies: RedditObject,
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
    override val ups: Int
) : RedditObject(), RedditSubmission

@JsonClass(generateAdapter = true)
internal data class RedditLink(
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
    override val ups: Int
) : RedditObject(), RedditSubmission

@JsonClass(generateAdapter = true)
internal data class RedditListing(
    val after: String,
    val before: String?,
    val children: List<RedditObject>,
    val modhash: String
) : RedditObject()
