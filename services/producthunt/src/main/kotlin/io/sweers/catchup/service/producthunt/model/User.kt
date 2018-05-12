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

package io.sweers.catchup.service.producthunt.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Models a user on Product Hunt.
 */
@JsonClass(generateAdapter = true)
internal data class User(
    @Json(name = "created_at") val createdAt: String,
    val headline: String?,
    val id: Long,
    @Json(name = "image_url") val imageUrl: Map<String, String>,
    val name: String,
    @Json(name = "profile_url") val profileUrl: String,
    val username: String,
    @Json(name = "website_url") val websiteUrl: String?
)
