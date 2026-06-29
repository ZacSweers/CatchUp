/*
 * Copyright (C) 2026. Zac Sweers
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
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
package catchup.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

/**
 * Returns the previous value of [current].
 *
 * Adapted from http://www.billjings.net/posts/title/the-unholy-composable/?up=technical
 */
@Composable
fun <R, T : R> previous(current: T, initial: R): R {
  val lastValue = remember { mutableStateOf(initial) }
  return remember(current) {
    val previous = lastValue.value
    lastValue.value = current
    previous
  }
}
