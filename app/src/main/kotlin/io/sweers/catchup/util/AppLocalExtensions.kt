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

package io.sweers.catchup.util

import android.app.Activity
import android.content.Context
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import io.sweers.catchup.P
import io.sweers.catchup.R

fun Activity.updateNavBarColor(
    @ColorInt color: Int? = null,
    context: Context = this,
    recreate: Boolean = false) {
  // Update the nav bar with whatever prefs we had
  @Suppress("CascadeIf") // Because I think if-else makes more sense readability-wise
  if (color != null && P.ThemeNavigationBar.get()) {
    window.navigationBarColor = color
  } else if (recreate) {
    recreate()
  } else {
    // SOME devices have naturally light status bars, try to cover for that here if we're in
    // night mode
    val currentColor = window.navigationBarColor
    if (context.isInNightMode() && !ColorUtils.isDark(currentColor)) {
      if (!ColorUtils.isDark(currentColor)) {
        window.navigationBarColor = ContextCompat.getColor(context,
            R.color.colorPrimaryDark)
      }
    }
  }
}
