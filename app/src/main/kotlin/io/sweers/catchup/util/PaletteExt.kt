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

@file:Suppress("NOTHING_TO_INLINE")

package io.sweers.catchup.util

import androidx.annotation.FloatRange
import androidx.palette.graphics.Palette
import androidx.palette.graphics.Palette.Swatch

/*
 * Extension functions for the Palette library.
 */

inline fun Palette.orderedSwatches(
    @FloatRange(from = 0.0, to = 1.0) darkAlpha: Float,
    @FloatRange(from = 0.0, to = 1.0) lightAlpha: Float): List<Pair<Swatch, Float>> {
  return listOfNotNull(
      vibrantSwatch?.let { it to darkAlpha },
      lightVibrantSwatch?.let { it to lightAlpha },
      darkVibrantSwatch?.let { it to darkAlpha },
      mutedSwatch?.let { it to darkAlpha },
      lightMutedSwatch?.let { it to lightAlpha },
      darkMutedSwatch?.let { it to darkAlpha }
  )
}

inline fun Palette.findSwatch(predicate: (Swatch) -> Boolean): Swatch? {
  return listOfNotNull(
      darkVibrantSwatch,
      lightMutedSwatch,
      vibrantSwatch,
      mutedSwatch,
      lightVibrantSwatch,
      darkMutedSwatch
  )
      .firstOrNull(predicate)
}

val Swatch.hue: Float
  get() = hsl[0]
val Swatch.saturation: Float
  get() = hsl[1]
val Swatch.luminosity: Float
  get() = hsl[2]
