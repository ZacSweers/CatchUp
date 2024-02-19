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
@file:Suppress("NOTHING_TO_INLINE")

package catchup.app.util

import androidx.annotation.ColorInt
import androidx.annotation.FloatRange
import androidx.palette.graphics.Palette
import catchup.base.ui.ColorUtils
import catchup.base.ui.orderedSwatches

object UiUtil {

  @ColorInt
  inline fun createRippleColor(
    palette: Palette,
    @FloatRange(from = 0.0, to = 1.0) darkAlpha: Float,
    @FloatRange(from = 0.0, to = 1.0) lightAlpha: Float,
    @ColorInt fallbackColor: Int,
  ): Int {
    // try the named swatches in preference order
    return palette.orderedSwatches(darkAlpha, lightAlpha).firstOrNull()?.let { (swatch, alpha) ->
      return@let ColorUtils.modifyAlpha(swatch.rgb, alpha)
    } ?: fallbackColor
  }
}
