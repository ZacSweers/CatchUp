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

import android.util.Property

/**
 * An implementation of [Property] to be used specifically with fields of
 * type `float`. This type-specific subclass enables performance benefit by allowing
 * calls to a [set()][.set] function that takes the primitive
 * `float` type and avoids autoboxing and other overhead associated with the
 * `Float` class.
 *
 * @param <T> The class on which the Property is declared.
 */
abstract class FloatProperty<T>(name: String) : Property<T, Float>(Float::class.java, name) {

  /**
   * A type-specific override of the [.set] that is faster when dealing
   * with fields of type `float`.
   */
  abstract fun setValue(target: T, value: Float)

  override fun set(target: T, value: Float?) {
    setValue(target, value!!)
  }
}
