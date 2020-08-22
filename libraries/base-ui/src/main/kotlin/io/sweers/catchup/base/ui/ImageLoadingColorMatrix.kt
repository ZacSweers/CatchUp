/*
 * Copyright (C) 2020. Zac Sweers
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
package io.sweers.catchup.base.ui

import android.graphics.ColorMatrix
import android.util.Property

/**
 * An extension to [ColorMatrix] which implements the Material image loading pattern
 */
class ImageLoadingColorMatrix : ColorMatrix() {
  private val elements = FloatArray(20)

  var saturationFraction = 1f
    set(value) {
      if (value != field) {
        System.arraycopy(array, 0, elements, 0, 20)

        // Taken from ColorMatrix.setSaturation. We can't use that though since it resets the matrix
        // before applying the values
        val invSat = 1 - value
        val r = 0.213f * invSat
        val g = 0.715f * invSat
        val b = 0.072f * invSat

        elements[0] = r + value
        elements[1] = g
        elements[2] = b
        elements[5] = r
        elements[6] = g + value
        elements[7] = b
        elements[10] = r
        elements[11] = g
        elements[12] = b + value

        set(elements)
      }
      field = value
    }

  var alphaFraction = 1f
    set(value) {
      if (value != field) {
        System.arraycopy(array, 0, elements, 0, 20)
        elements[18] = value
        set(elements)
      }
      field = value
    }

  var brightnessFraction = 1f
    set(value) {
      if (value != field) {
        System.arraycopy(array, 0, elements, 0, 20)

        // We substract to make the picture look darker, it will automatically clamp
        val darkening = (1 - value) * 255
        elements[4] = -darkening
        elements[9] = -darkening
        elements[14] = -darkening

        set(elements)
      }
      field = value
    }

  companion object {
    private val saturationFloatProp: FloatProp<ImageLoadingColorMatrix> = object : FloatProp<ImageLoadingColorMatrix>("saturation") {
      override operator fun get(o: ImageLoadingColorMatrix): Float = o.saturationFraction
      override operator fun set(o: ImageLoadingColorMatrix, value: Float) {
        o.saturationFraction = value
      }
    }

    private val alphaFloatProp: FloatProp<ImageLoadingColorMatrix> = object : FloatProp<ImageLoadingColorMatrix>("alpha") {
      override operator fun get(o: ImageLoadingColorMatrix): Float = o.alphaFraction
      override operator fun set(o: ImageLoadingColorMatrix, value: Float) {
        o.alphaFraction = value
      }
    }

    private val brightnessFloatProp: FloatProp<ImageLoadingColorMatrix> = object : FloatProp<ImageLoadingColorMatrix>("darken") {
      override operator fun get(o: ImageLoadingColorMatrix): Float = o.brightnessFraction
      override operator fun set(o: ImageLoadingColorMatrix, value: Float) {
        o.brightnessFraction = value
      }
    }

    val PROP_SATURATION: Property<ImageLoadingColorMatrix, Float> = createFloatProperty(saturationFloatProp)
    val PROP_ALPHA: Property<ImageLoadingColorMatrix, Float> = createFloatProperty(alphaFloatProp)
    val PROP_BRIGHTNESS: Property<ImageLoadingColorMatrix, Float> = createFloatProperty(brightnessFloatProp)
  }
}
