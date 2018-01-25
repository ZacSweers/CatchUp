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

package io.sweers.catchup.service.dribbble.model

import com.squareup.moshi.Json
import io.sweers.moshkt.api.MoshiSerializable
import org.threeten.bp.Instant

/**
 * Models a dibbble shot
 */
@MoshiSerializable
internal data class Shot(val animated: Boolean,
    @Json(name = "attachments_count") val attachmentsCount: Long,
    @Json(name = "attachments_url") val attachmentsUrl: String,
    @Json(name = "buckets_count") val bucketsCount: Long,
    @Json(name = "buckets_url") val bucketsUrl: String,
    @Json(name = "comments_count") val commentsCount: Long,
    @Json(name = "comments_url") val commentsUrl: String,
    @Json(name = "created_at") val createdAt: Instant,
    val description: String?,
    val height: Long,
    @Json(name = "html_url") val htmlUrl: String,
    val id: Long,
    val images: Images,
    @Json(name = "likes_count") val likesCount: Long,
    @Json(name = "likes_url") val likesUrl: String,
    @Json(name = "projects_url") val projectsUrl: String,
    @Json(name = "rebounds_count") val reboundsCount: Long,
    @Json(name = "rebounds_url") val reboundsUrl: String,
    val tags: List<String>,
//    val team: Team?, // TODO we get json arrays for this sometimes??
    @Json(name = "updated_at") val updatedAt: Instant,
    val user: User,
    @Json(name = "views_count") val viewsCount: Long,
    val width: Long)
