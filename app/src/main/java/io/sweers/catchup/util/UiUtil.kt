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

package io.sweers.catchup.util

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Build
import android.support.annotation.ColorInt
import android.support.annotation.FloatRange
import android.support.v7.graphics.Palette
import android.support.v7.graphics.Palette.Swatch

object UiUtil {

  /**
   * Creates a selector drawable that is API-aware. This will create a ripple for Lollipop+ and
   * supports masks. If this is pre-lollipop and no mask is provided, it will fall back to a simple
   * [StateListDrawable] with the color as its pressed and focused states.
   *
   * @param color Selector color
   * @param mask Mask drawable for ripples to be bound to
   * @return The drawable if successful, or null if not valid for this case (masked on pre-lollipop)
   */
  fun createColorSelector(@ColorInt color: Int,
      mask: Drawable?): Drawable? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      return RippleDrawable(ColorStateList.valueOf(color), null, mask)
    } else if (mask == null) {
      val colorDrawable = ColorDrawable(color)
      val statefulDrawable = StateListDrawable()
      statefulDrawable.setEnterFadeDuration(200)
      statefulDrawable.setExitFadeDuration(200)
      statefulDrawable.addState(intArrayOf(android.R.attr.state_pressed), colorDrawable)
      statefulDrawable.addState(intArrayOf(android.R.attr.state_focused), colorDrawable)
      statefulDrawable.addState(intArrayOf(), null)
      return statefulDrawable
    } else {
      // We don't do it on pre-lollipop because normally selectors can't abide by a mask
      return null
    }
  }

  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  fun createRipple(@ColorInt color: Int, bounded: Boolean): RippleDrawable {
    return RippleDrawable(ColorStateList.valueOf(color), null,
        if (bounded) ColorDrawable(Color.WHITE) else null)
  }

  @SuppressLint("Range")
  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  fun createRipple(@ColorInt inputColor: Int,
      @FloatRange(from = 0.0, to = 1.0) alpha: Float,
      bounded: Boolean): RippleDrawable {
    var color = inputColor
    color = ColorUtils.modifyAlpha(color, alpha)
    return RippleDrawable(ColorStateList.valueOf(color), null,
        if (bounded) ColorDrawable(Color.WHITE) else null)
  }

  @SuppressLint("Range")
  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  fun createRipple(palette: Palette,
      @FloatRange(from = 0.0, to = 1.0) darkAlpha: Float,
      @FloatRange(from = 0.0, to = 1.0) lightAlpha: Float,
      @ColorInt fallbackColor: Int,
      bounded: Boolean): RippleDrawable {
    // try the named swatches in preference order
    val rippleColor = palette.orderedSwatches(darkAlpha, lightAlpha)
        .firstOrNull()
        ?.let { (swatch, alpha) ->
          return@let ColorUtils.modifyAlpha(swatch.rgb, alpha)
        } ?: fallbackColor
    return RippleDrawable(ColorStateList.valueOf(rippleColor), null,
        if (bounded) ColorDrawable(Color.WHITE) else null)
  }
}

fun Palette.orderedSwatches(
    @FloatRange(from = 0.0, to = 1.0) darkAlpha: Float,
    @FloatRange(from = 0.0, to = 1.0) lightAlpha: Float): List<Pair<Swatch, Float>> {
  return listOf(
      vibrantSwatch?.let { Pair(it, darkAlpha) },
      lightVibrantSwatch?.let { Pair(it, lightAlpha) },
      darkVibrantSwatch?.let { Pair(it, darkAlpha) },
      mutedSwatch?.let { Pair(it, darkAlpha) },
      lightMutedSwatch?.let { Pair(it, lightAlpha) },
      darkMutedSwatch?.let { Pair(it, darkAlpha) }
  ).filterNotNull()
}
