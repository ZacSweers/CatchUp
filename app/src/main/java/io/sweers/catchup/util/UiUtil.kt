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

@file:Suppress("NOTHING_TO_INLINE")

package io.sweers.catchup.util

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Build
import android.support.annotation.ColorInt
import android.support.annotation.FloatRange
import android.support.v4.view.animation.FastOutSlowInInterpolator
import android.support.v7.app.AppCompatDelegate
import android.support.v7.graphics.Palette
import android.support.v7.graphics.Palette.Swatch
import io.sweers.catchup.P


object UiUtil {

  inline fun updateNightMode(activity: Activity) {
    // TODO Why doesn't this work as an extension function? IDE doesn't see it
    val isCurrentlyInNightMode = activity.isInNightMode()
    val nightMode = when {
      P.DaynightAuto.get() -> AppCompatDelegate.MODE_NIGHT_AUTO
      P.DaynightNight.get() -> AppCompatDelegate.MODE_NIGHT_YES
      else -> AppCompatDelegate.MODE_NIGHT_NO
    }
    if ((nightMode != AppCompatDelegate.MODE_NIGHT_YES && isCurrentlyInNightMode)
        || nightMode == AppCompatDelegate.MODE_NIGHT_YES && !isCurrentlyInNightMode) {
      AppCompatDelegate.setDefaultNightMode(nightMode)
      activity.recreate()
    }
  }

  /**
   * Creates a selector drawable that is API-aware. This will create a ripple for Lollipop+ and
   * supports masks. If this is pre-lollipop and no mask is provided, it will fall back to a simple
   * [StateListDrawable] with the color as its pressed and focused states.
   *
   * @param color Selector color
   * @param mask Mask drawable for ripples to be bound to
   * @return The drawable if successful, or null if not valid for this case (masked on pre-lollipop)
   */
  inline fun createColorSelector(@ColorInt color: Int,
      mask: Drawable?): Drawable? {
    return when {
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP -> RippleDrawable(
          ColorStateList.valueOf(color), null, mask)
      mask == null -> {
        val colorDrawable = ColorDrawable(color)
        val statefulDrawable = StateListDrawable()
        statefulDrawable.setEnterFadeDuration(200)
        statefulDrawable.setExitFadeDuration(200)
        statefulDrawable.addState(intArrayOf(android.R.attr.state_pressed), colorDrawable)
        statefulDrawable.addState(intArrayOf(android.R.attr.state_focused), colorDrawable)
        statefulDrawable.addState(intArrayOf(), null)
        statefulDrawable
      }
      else -> // We don't do it on pre-lollipop because normally selectors can't abide by a mask
        null
    }
  }

  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  inline fun createRipple(@ColorInt color: Int, bounded: Boolean): RippleDrawable {
    return RippleDrawable(ColorStateList.valueOf(color), null,
        if (bounded) ColorDrawable(Color.WHITE) else null)
  }

  @SuppressLint("Range")
  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  inline fun createRipple(@ColorInt inputColor: Int,
      @FloatRange(from = 0.0, to = 1.0) alpha: Float,
      bounded: Boolean): RippleDrawable {
    var color = inputColor
    color = ColorUtils.modifyAlpha(color, alpha)
    return RippleDrawable(ColorStateList.valueOf(color), null,
        if (bounded) ColorDrawable(Color.WHITE) else null)
  }

  @SuppressLint("Range")
  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  inline fun createRipple(palette: Palette,
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

inline fun Palette.orderedSwatches(
    @FloatRange(from = 0.0, to = 1.0) darkAlpha: Float,
    @FloatRange(from = 0.0, to = 1.0) lightAlpha: Float): List<Pair<Swatch, Float>> {
  return listOf(
      vibrantSwatch?.let { it to darkAlpha },
      lightVibrantSwatch?.let { it to lightAlpha },
      darkVibrantSwatch?.let { it to darkAlpha },
      mutedSwatch?.let { it to darkAlpha },
      lightMutedSwatch?.let { it to lightAlpha },
      darkMutedSwatch?.let { it to darkAlpha }
  ).filterNotNull()
}

inline fun Palette.orderedSwatches(predicate: (Swatch) -> Boolean): Swatch? {
  return listOf(
      darkVibrantSwatch,
      lightMutedSwatch,
      vibrantSwatch,
      mutedSwatch,
      lightVibrantSwatch,
      darkMutedSwatch
  ).filterNotNull().firstOrNull(predicate)
}

val fastOutSlowInInterpolator = FastOutSlowInInterpolator()
