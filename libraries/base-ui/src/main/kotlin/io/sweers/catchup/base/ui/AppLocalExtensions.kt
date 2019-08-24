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
package io.sweers.catchup.base.ui

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.os.Build
import androidx.annotation.ColorInt
import io.sweers.catchup.util.clearLightNavBar
import io.sweers.catchup.util.isInNightMode
import io.sweers.catchup.util.sdk
import io.sweers.catchup.util.setLightNavBar

private typealias MaterialAttr = com.google.android.material.R.attr

fun Activity.updateNavBarColor(
  @ColorInt color: Int? = null,
  context: Context = this,
  recreate: Boolean = false,
  uiPreferences: UiPreferences
) {
  // Update the nav bar with whatever prefs we had
  @Suppress("CascadeIf") // Because I think if-else makes more sense readability-wise
  if (color != null && uiPreferences.themeNavigationBar) {
    window.navigationBarColor = color
    window.navigationBarDividerColor = Color.TRANSPARENT
    window.decorView.clearLightNavBar() // TODO why do I need to do this every time?
  } else if (recreate) {
    recreate()
  } else {
    val currentColor = window.navigationBarColor
    val isDark = ColorUtils.isDark(currentColor)
    if (Build.VERSION.SDK_INT < 26) {
      // SOME devices have naturally light status bars, try to cover for that here if we're in
      // night mode
      if (context.isInNightMode() && !isDark) {
        window.navigationBarColor = MaterialColors.getColor(context, MaterialAttr.colorPrimaryDark, "No colorPrimaryDark found!")
      }
    } else {
      if (context.isInNightMode()) {
        window.navigationBarColor = MaterialColors.getColor(context, MaterialAttr.colorPrimaryDark, "No colorPrimaryDark found!")
        sdk(27) {
          window.navigationBarDividerColor = Color.TRANSPARENT
        }
        window.decorView.clearLightNavBar()
      } else {
        window.navigationBarColor = MaterialColors.getColor(context, MaterialAttr.colorPrimary, "No colorPrimaryDark found!")
        sdk(27) {
          window.navigationBarDividerColor = MaterialColors.getColor(context, MaterialAttr.colorPrimaryDark, "No colorPrimaryDark found!")
        }
        window.decorView.setLightNavBar()
      }
    }
  }
}
