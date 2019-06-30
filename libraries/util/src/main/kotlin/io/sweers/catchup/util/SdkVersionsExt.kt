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

import android.os.Build

fun isM(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
fun isN(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
fun isO(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
fun isOMR1(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1
fun isP(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P

// Not totally safe to use yet
// https://issuetracker.google.com/issues/64550633
inline fun <T> sdk(level: Int, func: () -> T): T? {
  return if (Build.VERSION.SDK_INT >= level) {
    func.invoke()
  } else {
    null
  }
}
