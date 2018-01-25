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
import java.util.Date

/**
 * Models a commend on a Dribbble shot.
 */
@MoshiSerializable
internal data class Comment(val body: String,
    @Json(name = "created_at") val createdAt: Instant,
    val id: Long,
    @Json(name = "likes_count") val likesCount: Long,
    @Json(name = "likes_url") val likesUrl: String,
    @Json(name = "updated_at") val updatedAt: Date,
    val user: User)
