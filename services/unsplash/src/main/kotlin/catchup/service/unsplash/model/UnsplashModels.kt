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
package catchup.service.unsplash.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.datetime.Instant

@JsonClass(generateAdapter = true)
data class UnsplashPhoto(
  val id: String,
  @Json(name = "created_at") val createdAt: Instant,
  // Hex color
  val color: String,
  val description: String?,
  val urls: Urls,
  val links: Links,
  val likes: Int,
  val user: User,
  @Json(name = "blur_hash") val blurHash: String? = null,
  val height: Int,
  val width: Int,
)

@JsonClass(generateAdapter = true)
data class Urls(
  val raw: String,
  val full: String,
  val regular: String,
  val small: String,
  val thumb: String,
)

@JsonClass(generateAdapter = true)
data class Links(
  val self: String,
  val html: String,
  val download: String,
  @Json(name = "download_location") val downloadLocation: String,
)

@JsonClass(generateAdapter = true)
data class User(val id: String, val name: String, val username: String)
