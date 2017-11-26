/*
 * Copyright (c) 2017 Zac Sweers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:Suppress("NOTHING_TO_INLINE")

package io.sweers.catchup.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.support.v7.app.AppCompatDelegate
import io.sweers.catchup.P

inline fun Activity.updateNightMode() {
  val isCurrentlyInNightMode = isInNightMode()
  val nightMode = when {
    P.DaynightAuto.get() -> AppCompatDelegate.MODE_NIGHT_AUTO
    P.DaynightNight.get() -> AppCompatDelegate.MODE_NIGHT_YES
    else -> AppCompatDelegate.MODE_NIGHT_NO
  }
  if (nightMode == AppCompatDelegate.MODE_NIGHT_AUTO
      || (isCurrentlyInNightMode && nightMode != AppCompatDelegate.MODE_NIGHT_YES)
      || !isCurrentlyInNightMode && nightMode == AppCompatDelegate.MODE_NIGHT_YES) {
    AppCompatDelegate.setDefaultNightMode(nightMode)
    recreate()
  }
}

fun Context.resolveActivity(): Activity {
  if (this is Activity) {
    return this
  }
  if (this is ContextWrapper) {
    return baseContext.resolveActivity()
  }
  throw UnsupportedOperationException("Given context was not an activity! Is a $this")
}
