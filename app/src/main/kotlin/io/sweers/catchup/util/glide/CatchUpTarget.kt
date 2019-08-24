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
package io.sweers.catchup.util.glide

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.palette.graphics.Palette
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.transition.Transition
import io.sweers.catchup.ui.widget.BadgedFourThreeImageView
import io.sweers.catchup.base.ui.ColorUtils
import io.sweers.catchup.util.UiUtil
import io.sweers.catchup.util.generateAsync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * A Glide {@see ViewTarget} for [BadgedFourThreeImageView]s. It applies a badge for animated
 * images, can prevent GIFs from auto-playing & applies a palette generated ripple.
 */
class CatchUpTarget(
  view: BadgedFourThreeImageView,
  private val autoplayGifs: Boolean,
  private val scope: CoroutineScope
) : NonAutoStartDrawableImageViewTarget(view) {

  override fun onResourceReady(
    resource: Drawable,
    transition: Transition<in Drawable>?
  ) {
    super.onResourceReady(resource, transition)
    if (autoplayGifs && resource is GifDrawable) {
      resource.start()
    }

    val badgedImageView = getView() as BadgedFourThreeImageView
    if (resource is BitmapDrawable) {
      scope.launch {
        Palette.from(resource.bitmap)
            .clearFilters()
            .generateAsync()?.let(::applyPalette)
      }
    } else if (resource is GifDrawable) {
      val image = resource.firstFrame
      if (image == null || image.isRecycled) {
        return
      }
      scope.launch {
        Palette.from(image).clearFilters().generateAsync()?.let(::applyPalette)
      }

      // look at the corner to determine the gif badge color
      val cornerSize = (56 * getView().context.resources
          .displayMetrics.scaledDensity).toInt()
      val corner = Bitmap.createBitmap(image,
          image.width - cornerSize,
          image.height - cornerSize,
          cornerSize, cornerSize)
      val isDark = ColorUtils.isDark(corner)
      corner.recycle()
      badgedImageView.setBadgeColor(if (isDark) 0xb3ffffff.toInt() else 0x40000000)
    }
  }

  private fun applyPalette(palette: Palette) {
    (getView() as BadgedFourThreeImageView).foreground =
        UiUtil.createRipple(palette, 0.25f, 0.5f, 0x40808080, true)
  }
}
