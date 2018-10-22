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
import android.graphics.Color
import android.os.Build
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import com.f2prateek.rx.preferences2.Preference
import io.sweers.catchup.R
import io.sweers.catchup.injection.scopes.PerActivity
import io.sweers.catchup.preferences.invoke
import javax.inject.Inject
import javax.inject.Qualifier
import kotlin.annotation.AnnotationRetention.BINARY
import kotlin.annotation.AnnotationTarget.FUNCTION
import kotlin.annotation.AnnotationTarget.VALUE_PARAMETER

@Qualifier
@Target(VALUE_PARAMETER, FUNCTION)
@Retention(BINARY)
annotation class ThemeNavBar

@PerActivity
class NavBarColorizer @Inject constructor(
    private val activity: Activity,
    @param:ThemeNavBar private val themeNavBar: Preference<Boolean>) {

  fun refresh(
      @ColorInt color: Int? = null,
      context: Context = activity,
      recreate: Boolean = false) {
    // Update the nav bar with whatever prefs we had
    @Suppress("CascadeIf") // Because I think if-else makes more sense readability-wise
    if (themeNavBar() && color != null) {
      activity.window.navigationBarColor = color
      activity.window.navigationBarDividerColor = Color.TRANSPARENT
      activity.window.decorView.clearLightNavBar() // TODO why do I need to do this every time?
    } else if (recreate) {
      activity.recreate()
    } else {
      val currentColor = activity.window.navigationBarColor
      val isDark = ColorUtils.isDark(currentColor)
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
        // SOME devices have naturally light status bars, try to cover for that here if we're in
        // night mode
        if (context.isInNightMode() && !isDark) {
          activity.window.navigationBarColor = ContextCompat.getColor(context,
              R.color.colorPrimaryDark)
        }
      } else {
        if (context.isInNightMode()) {
          activity.window.navigationBarColor = ContextCompat.getColor(context,
              R.color.colorPrimaryDark)
          sdk(Build.VERSION_CODES.P) {
            activity.window.navigationBarDividerColor = Color.TRANSPARENT
          }
          activity.window.decorView.clearLightNavBar()
        } else {
          activity.window.navigationBarColor = ContextCompat.getColor(context,
              R.color.colorPrimary)
          sdk(Build.VERSION_CODES.P) {
            activity.window.navigationBarDividerColor = ContextCompat.getColor(context,
                R.color.colorPrimaryDark)
          }
          activity.window.decorView.setLightNavBar()
        }
      }
    }
  }
}
