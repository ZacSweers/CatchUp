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
@file:Suppress("MemberVisibilityCanPrivate")

package io.sweers.catchup.base.ui

import android.graphics.Bitmap
import androidx.annotation.CheckResult
import androidx.annotation.ColorInt
import androidx.annotation.FloatRange
import androidx.annotation.IntDef
import androidx.annotation.IntRange
import androidx.annotation.Size
import androidx.core.graphics.ColorUtils.colorToHSL
import androidx.palette.graphics.Palette
import io.sweers.catchup.base.ui.ColorUtils.Lightness
import kotlin.annotation.AnnotationRetention.SOURCE

/**
 * Checks if the most populous color in the given palette is dark
 *
 * Annoyingly we have to return this Lightness 'enum' rather than a boolean as palette isn't
 * guaranteed to find the most populous color.
 */
@Lightness
fun Palette.isDark(): Int {
  val mostPopulous = getMostPopulousSwatch() ?: return ColorUtils.LIGHTNESS_UNKNOWN
  return if (ColorUtils.isDark(mostPopulous.hsl)) ColorUtils.IS_DARK else ColorUtils.IS_LIGHT
}

fun Palette.getMostPopulousSwatch(): Palette.Swatch? {
  var mostPopulous: Palette.Swatch? = null
  for (swatch in swatches) {
    if (mostPopulous == null || swatch.population > mostPopulous.population) {
      mostPopulous = swatch
    }
  }
  return mostPopulous
}

/**
 * Utility methods for working with colors.
 *
 * TODO Properly split this up into extension functions
 */
object ColorUtils {

  @Retention(SOURCE)
  @IntDef(IS_LIGHT,
      IS_DARK,
      LIGHTNESS_UNKNOWN)
  annotation class Lightness

  const val IS_LIGHT = 0
  const val IS_DARK = 1
  const val LIGHTNESS_UNKNOWN = 2

  /**
   * Set the alpha component of `color` to be `alpha`.
   */
  @CheckResult
  @ColorInt
  fun modifyAlpha(
    @ColorInt color: Int,
    @IntRange(from = 0, to = 255) alpha: Int
  ): Int {
    return color and 0x00ffffff or (alpha shl 24)
  }

  /**
   * Set the alpha component of `color` to be `alpha`.
   */
  @CheckResult
  @ColorInt
  fun modifyAlpha(
    @ColorInt color: Int,
    @FloatRange(from = 0.0, to = 1.0) alpha: Float
  ): Int {
    return modifyAlpha(color, (255f * alpha).toInt())
  }

  /**
   * Determines if a given bitmap is dark. This extracts a palette inline so should not be called
   * with a large image!! If palette fails then check the color of the specified pixel
   */
  fun isDark(
    bitmap: Bitmap,
    backupPixelX: Int = bitmap.width / 2,
    backupPixelY: Int = bitmap.height / 2
  ): Boolean {
    // first try palette with a small color quant size
    val palette = Palette.from(bitmap).maximumColorCount(3).generate()
    return if (palette.swatches.size > 0) {
      palette.isDark() == IS_DARK
    } else {
      // if palette failed, then check the color of the specified pixel
      isDark(bitmap.getPixel(backupPixelX, backupPixelY))
    }
  }

  /**
   * Check that the lightness value (0–1)
   */
  fun isDark(@Size(3) hsl: FloatArray): Boolean {
    return hsl[2] < 0.5f
  }

  /**
   * Convert to HSL & check that the lightness value
   */
  fun isDark(
    @ColorInt color: Int
  ): Boolean {
    val hsl = FloatArray(3)
    colorToHSL(color, hsl)
    return isDark(hsl)
  }
}
