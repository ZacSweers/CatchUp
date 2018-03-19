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
 * Models a dribbble user
 */
@MoshiSerializable
internal data class User(@Json(name = "avatar_url") val avatarUrl: String,
    val bio: String,
    @Json(name = "buckets_count") val bucketsCount: Int,
    @Json(name = "buckets_url") val bucketsUrl: String,
    @Json(name = "created_at") val createdAt: Instant,
    @Json(name = "followers_count") val followersCount: Int,
    @Json(name = "followers_url") val followersUrl: String,
    @Json(name = "following_url") val followingUrl: String,
    @Json(name = "followings_count") val followingsCount: Int,
    @Json(name = "html_url") val htmlUrl: String,
    val id: Long,
    @Json(name = "likes_count") val likesCount: Int,
    @Json(name = "likes_url") val likesUrl: String,
    val links: Map<String, String>,
    val location: String?,
    val name: String,
    val pro: Boolean?,
    @Json(name = "projects_count") val projectsCount: Int,
    @Json(name = "projects_url") val projectsUrl: String,
    @Json(name = "shots_count") val shotsCount: Int,
    @Json(name = "shots_url") val shotsUrl: String,
    @Json(name = "teams_count") val teamsCount: Int?,
    @Json(name = "teams_url") val teamsUrl: String?,
    val type: String,
    @Json(name = "updated_at") val updatedAt: Instant,
    val username: String)
