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

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.view.View

fun View.setLightStatusBar() {
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
    var flags = systemUiVisibility
    // TODO noop if it's already set
    flags = flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
    systemUiVisibility = flags
  }
}

fun View.clearLightStatusBar() {
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
    var flags = systemUiVisibility
    // TODO noop if it's already not set
    flags = flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
    systemUiVisibility = flags
  }
}

fun View.setLightNavBar() {
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    var flags = systemUiVisibility
    // TODO noop if it's already set
    flags = flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
    systemUiVisibility = flags
  }
}

fun View.clearLightNavBar() {
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    var flags = systemUiVisibility
    // TODO noop if it's already not set
    flags = flags and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
    systemUiVisibility = flags
  }
}

inline fun View.show() {
  visibility = View.VISIBLE
}

inline infix fun View.showIf(condition: Boolean) {
  if (condition) {
    show()
  } else {
    hide()
  }
}

inline fun View.hide() {
  visibility = View.GONE
}

inline infix fun View.hideIf(condition: Boolean) {
  if (condition) {
    hide()
  } else {
    show()
  }
}

fun Context.asDayContext(): Context {
  return if (isInNightMode()) {
    createConfigurationContext(
        Configuration(resources.configuration)
            .apply { uiMode = Configuration.UI_MODE_NIGHT_NO })
  } else this
}
