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
package catchup.util

import android.content.Context
import android.content.res.Configuration

fun Context.toDayContext(): Context {
  return if (isInNightMode()) {
    copy(isInNightMode = false)
  } else {
    this
  }
}

fun Context.toNightContext(): Context {
  return if (isInNightMode()) {
    this
  } else {
    copy(isInNightMode = true)
  }
}

private fun Context.copy(isInNightMode: Boolean): Context {
  val config =
    Configuration(resources.configuration).apply {
      val uiModeFlag =
        if (isInNightMode) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
      uiMode = uiModeFlag or (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv())
    }
  return createConfigurationContext(config)
}
