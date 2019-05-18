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
package io.sweers.catchup.service.api

import android.content.Context
import android.net.Uri
import android.view.View
import androidx.annotation.ColorInt

data class UrlMeta(
  val uri: Uri?,
  @ColorInt val accentColor: Int,
  val context: Context,
  val imageViewerData: ImageViewerData? = null
) {

  constructor(
    url: String?,
    @ColorInt accentColor: Int,
    context: Context,
    imageViewerData: ImageViewerData? = null
  ) : this(
      if (url.isNullOrBlank()) null else Uri.parse(url), accentColor, context, imageViewerData)

  fun isSupportedInMediaViewer(): Boolean {
    return uri?.toString()?.let {
      val extension = it.substring(it.lastIndexOf(".") + 1)
      extension in MEDIA_EXTENSIONS
    } ?: false
  }

  companion object {
    // webp - https://github.com/zjupure/GlideWebpDecoder
    // videos? idk if I want to write a video player :|
    private val MEDIA_EXTENSIONS = setOf("gif", "jpg", "jpeg", "png", "raw")
  }
}

data class ImageViewerData(
  val id: String,
  val imageUrl: String,
  val sourceUrl: String,
  val image: View
)
