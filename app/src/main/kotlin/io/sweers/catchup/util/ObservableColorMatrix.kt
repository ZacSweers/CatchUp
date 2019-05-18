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
package io.sweers.catchup.util

import android.graphics.ColorMatrix
import android.util.Property

/**
 * An extension to [ColorMatrix] which caches the saturation value for animation purposes.
 */
class ObservableColorMatrix : ColorMatrix() {
  private var saturation = 1f

  fun getSaturation(): Float {
    return saturation
  }

  override fun setSaturation(saturation: Float) {
    this.saturation = saturation
    super.setSaturation(saturation)
  }

  companion object {

    val SATURATION: Property<ObservableColorMatrix, Float> = object : FloatProperty<ObservableColorMatrix>(
        "saturation") {

      override fun setValue(target: ObservableColorMatrix, value: Float) {
        target.setSaturation(value)
      }

      override fun get(cm: ObservableColorMatrix): Float {
        return cm.getSaturation()
      }
    }
  }
}
