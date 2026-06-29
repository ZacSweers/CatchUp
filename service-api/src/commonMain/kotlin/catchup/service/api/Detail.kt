/*
 * Copyright (C) 2026. Zac Sweers
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
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
package catchup.service.api

import androidx.compose.runtime.Immutable
import kotlin.time.Instant
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

// TODO
//  sort?
@Immutable
sealed interface Detail {
  val id: String
  val itemId: Long
  val title: String
  val text: String?
  val imageUrl: String?
  val score: Long?
  val commentsCount: Int?
  val linkUrl: String?
  val shareUrl: String?
  val tag: String?
  val author: String?
  val timestamp: Instant?
  val allowUnfurl: Boolean

  data class Shallow(
    override val id: String,
    override val itemId: Long,
    override val title: String,
    override val text: String? = null,
    override val imageUrl: String? = null,
    override val score: Long? = null,
    override val commentsCount: Int? = null,
    override val linkUrl: String? = null,
    override val shareUrl: String? = null,
    override val tag: String? = null,
    override val author: String? = null,
    override val timestamp: Instant? = null,
    override val allowUnfurl: Boolean = true,
  ) : Detail

  data class Full(
    override val id: String,
    override val itemId: Long,
    override val title: String,
    override val text: String? = null,
    override val imageUrl: String? = null,
    override val score: Long? = null,
    override val commentsCount: Int? = null,
    override val linkUrl: String? = null,
    override val shareUrl: String,
    override val tag: String? = null,
    override val author: String? = null,
    override val timestamp: Instant? = null,
    override val allowUnfurl: Boolean = true,
    val comments: ImmutableList<Comment> = persistentListOf(),
  ) : Detail
}

@Immutable
data class Comment(
  val id: String,
  val serviceId: String,
  val author: String,
  val timestamp: Instant,
  val text: String,
  val score: Int,
  val children: List<Comment>,
  val depth: Int = 0,
  val clickableUrls: List<ClickableUrl>,
)

@Immutable data class ClickableUrl(val text: String, val url: String, val previewUrl: String?)
