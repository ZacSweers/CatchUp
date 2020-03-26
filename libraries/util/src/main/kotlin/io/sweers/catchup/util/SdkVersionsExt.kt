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
import dev.zacsweers.catchup.appconfig.AppConfig

fun AppConfig.isM(): Boolean = sdkInt >= Build.VERSION_CODES.M
fun AppConfig.isN(): Boolean = sdkInt >= Build.VERSION_CODES.N
fun AppConfig.isO(): Boolean = sdkInt >= Build.VERSION_CODES.O
fun AppConfig.isOMR1(): Boolean = sdkInt >= Build.VERSION_CODES.O_MR1
fun AppConfig.isP(): Boolean = sdkInt >= Build.VERSION_CODES.P

// Not totally safe to use yet
// https://issuetracker.google.com/issues/64550633
// TODO move to AppConfig extension
inline fun <T> sdk(level: Int, func: () -> T): T? {
  return if (Build.VERSION.SDK_INT >= level) {
    func()
  } else {
    null
  }
}
