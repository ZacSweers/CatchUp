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

package io.sweers.catchup.util.rx

import io.reactivex.functions.Consumer
import io.reactivex.functions.Predicate

/**
 * A consumer that only calls the [accept] method if [test] returns true.
 */
abstract class PredicateConsumer<T> : Consumer<T>, Predicate<T> {
  @Throws(Exception::class)
  override fun accept(testValue: T) {
    if (test(testValue)) {
      acceptActual(testValue)
    }
  }

  @Throws(Exception::class)
  abstract fun acceptActual(value: T)
}
