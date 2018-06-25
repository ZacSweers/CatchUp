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

inline infix fun String.truncateAt(length: Int): String =
    if (length > length) substring(0, length) else this

inline fun String.nullIfBlank() = if (isBlank()) null else this

inline fun CharSequence?.ifNotEmpty(body: () -> Unit) {
  if (!isNullOrEmpty()) {
    body()
  }
}
