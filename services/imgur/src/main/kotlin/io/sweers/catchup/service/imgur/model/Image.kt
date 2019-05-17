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
package io.sweers.catchup.service.imgur.model

import android.app.ActivityManager
import android.content.Context
import android.content.res.Configuration
import androidx.core.content.getSystemService
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.threeten.bp.Instant

@JsonClass(generateAdapter = true)
internal data class Image(
  val id: String,
  val title: String,
  val datetime: Instant,
  val cover: String?,
  val link: String,
  val downs: Int?,
  val type: String?,
  val ups: Int?,
  val score: Int?,
  @Json(name = "account_url") val accountUrl: String?,
  @Json(name = "account_id") val accountId: String?
) {

  fun resolveScore(): Int {
    score?.let { return it }
    ups?.let { ups ->
      downs?.let {
        return ups - it
      }
    }
    return 0
  }

  fun resolveClickLink() = link

  fun resolveDisplayLink(size: String = "l"): String {
    cover?.let { return "https://i.imgur.com/$it$size.webp" }
    val type = resolveType()
    return "https://i.imgur.com/$id$size.$type"
  }

  private fun resolveType(): String? {
    return when (type) {
      null -> null
      "image/gif" -> "gif"
      else -> "webp"
    }
  }

  companion object {
    /**
     * Not used yet as I'm not sure where to call this
     */
    fun resolveBestSize(context: Context): String {
//      t	Small Thumbnail	160x160	Yes
//      m	Medium Thumbnail	320x320	Yes
//      l	Large Thumbnail	640x640	Yes
//      h	Huge Thumbnail	1024x1024	Yes
      val smallLayout = context.resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK == Configuration.SCREENLAYOUT_SIZE_SMALL
      val lowRam = context.getSystemService<ActivityManager>()!!.isLowRamDevice
      return if (lowRam) {
        if (smallLayout) {
          "t"
        } else {
          "m"
        }
      } else {
        "l"
      }
    }
  }
}
