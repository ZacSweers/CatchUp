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

package io.sweers.catchup.ui.immersive

import android.annotation.TargetApi
import android.app.Activity
import android.os.Build
import android.view.View

@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
internal open class SystemUiHelperImplJB(activity: Activity,
    level: Int,
    flags: Int,
    onSystemUiVisibilityChangeListener: SystemUiHelper.OnSystemUiVisibilityChangeListener?) : SystemUiHelper.SystemUiHelperImpl(
    activity, level, flags,
    onSystemUiVisibilityChangeListener), View.OnSystemUiVisibilityChangeListener {

  protected val decorView: View = activity.window
      .decorView.apply {
    setOnSystemUiVisibilityChangeListener(this@SystemUiHelperImplJB)
  }

  protected fun createShowFlags(): Int {
    var flag = View.SYSTEM_UI_FLAG_VISIBLE
    if (level >= SystemUiHelper.LEVEL_HIDE_STATUS_BAR) {
      flag = flag or (View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)

      if (level >= SystemUiHelper.LEVEL_LEAN_BACK) {
        flag = flag or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
      }
    }
    return flag
  }

  protected open fun createHideFlags(): Int {
    var flag = View.SYSTEM_UI_FLAG_LOW_PROFILE
    if (level >= SystemUiHelper.LEVEL_LEAN_BACK) {
      flag = flag or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
    }

    if (level >= SystemUiHelper.LEVEL_HIDE_STATUS_BAR) {
      flag = flag or (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
          or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
          or View.SYSTEM_UI_FLAG_FULLSCREEN)

      if (level >= SystemUiHelper.LEVEL_LEAN_BACK) {
        flag = flag or (View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
      }
    }
    return flag
  }

  protected fun onSystemUiShown() {
    if (level == SystemUiHelper.LEVEL_LOW_PROFILE) {
      // Manually show the action bar when in low profile mode.
      val ab = activity.actionBar
      ab?.show()
    }
    isShowing = true
  }

  protected fun onSystemUiHidden() {
    if (level == SystemUiHelper.LEVEL_LOW_PROFILE) {
      // Manually hide the action bar when in low profile mode.
      val ab = activity.actionBar
      ab?.hide()
    }
    isShowing = false
  }

  protected fun createTestFlags(): Int {
    return if (level >= SystemUiHelper.LEVEL_LEAN_BACK) {
      // Intentionally override test flags.
      View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
    } else View.SYSTEM_UI_FLAG_LOW_PROFILE
  }

  override fun show() {
    decorView.systemUiVisibility = createShowFlags()
  }

  override fun hide() {
    decorView.systemUiVisibility = createHideFlags()
  }

  override fun onSystemUiVisibilityChange(visibility: Int) {
    if (visibility and createTestFlags() != 0) {
      onSystemUiHidden()
    } else {
      onSystemUiShown()
    }
  }
}
