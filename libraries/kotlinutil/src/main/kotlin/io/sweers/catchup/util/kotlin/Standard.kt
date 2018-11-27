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

package io.sweers.catchup.util.kotlin

inline fun <T, R> T.letIf(condition: Boolean, block: (T) -> R): R? = let {
  if (condition) {
    block(it)
  } else null
}

inline fun <T, R> T.runIf(condition: Boolean, block: T.() -> R): R? = run {
  if (condition) {
    block()
  } else null
}

inline fun <T> T.applyIf(condition: Boolean, block: T.() -> T): T = apply {
  if (condition) {
    block()
  }
}

inline fun <T> T.alsoIf(condition: Boolean, block: (T) -> T): T = also {
  if (condition) {
    block(it)
  }
}

inline fun <T> T.switch(block: T.() -> T): T = run {
  block()
}

inline fun <T> T.switchIf(condition: Boolean, block: T.() -> T): T = switch {
  if (condition) {
    block()
  } else this
}

/**
 * Applies a [block] on a set of [args].
 */
inline fun <T> applyOn(vararg args: T, crossinline block: T.() -> Unit) {
  args.asSequence().forEach { block(it) }
}

inline fun <T, R : T> Collection<R>.castUp() = this as Collection<T>

inline fun <R, T : R> Collection<R>.castDown() = this as Collection<T>

inline fun <T, R : T> List<R>.castUp() = this as List<T>

inline fun <R, T : R> List<R>.castDown() = this as List<T>

inline fun <T, R : T> Set<R>.castUp() = this as Set<T>

inline fun <R, T : R> Set<R>.castDown() = this as Set<T>
