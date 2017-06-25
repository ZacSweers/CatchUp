/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package io.sweers.catchup.ui.debug

import android.annotation.TargetApi
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets

/**
 * Provides functionality for DrawerLayout unique to API 21
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
internal object DrawerLayoutCompatApi21 {

  private val THEME_ATTRS = intArrayOf(android.R.attr.colorPrimaryDark)

  @JvmStatic
  fun configureApplyInsets(drawerLayout: View) {
    if (drawerLayout is DrawerLayoutImpl) {
      drawerLayout.setOnApplyWindowInsetsListener(InsetsListener())
      drawerLayout.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
    }
  }

  @JvmStatic
  fun dispatchChildInsets(child: View, insets: Any, gravity: Int) {
    var wi = insets as WindowInsets
    if (gravity == Gravity.LEFT) {
      wi = wi.replaceSystemWindowInsets(wi.systemWindowInsetLeft,
          wi.systemWindowInsetTop, 0, wi.systemWindowInsetBottom)
    } else if (gravity == Gravity.RIGHT) {
      wi = wi.replaceSystemWindowInsets(0, wi.systemWindowInsetTop,
          wi.systemWindowInsetRight, wi.systemWindowInsetBottom)
    }
    child.dispatchApplyWindowInsets(wi)
  }

  @JvmStatic
  fun applyMarginInsets(lp: ViewGroup.MarginLayoutParams, insets: Any,
      gravity: Int) {
    var wi = insets as WindowInsets
    if (gravity == Gravity.LEFT) {
      wi = wi.replaceSystemWindowInsets(wi.systemWindowInsetLeft,
          wi.systemWindowInsetTop, 0, wi.systemWindowInsetBottom)
    } else if (gravity == Gravity.RIGHT) {
      wi = wi.replaceSystemWindowInsets(0, wi.systemWindowInsetTop,
          wi.systemWindowInsetRight, wi.systemWindowInsetBottom)
    }
    lp.leftMargin = wi.systemWindowInsetLeft
    lp.topMargin = wi.systemWindowInsetTop
    lp.rightMargin = wi.systemWindowInsetRight
    lp.bottomMargin = wi.systemWindowInsetBottom
  }

  @JvmStatic
  fun getTopInset(insets: Any?): Int {
    return if (insets != null) (insets as WindowInsets).systemWindowInsetTop else 0
  }

  @JvmStatic
  fun getDefaultStatusBarBackground(context: Context): Drawable {
    val a = context.obtainStyledAttributes(THEME_ATTRS)
    try {
      return a.getDrawable(0)
    } finally {
      a.recycle()
    }
  }

  internal class InsetsListener : View.OnApplyWindowInsetsListener {
    override fun onApplyWindowInsets(v: View, insets: WindowInsets): WindowInsets {
      val drawerLayout = v as DrawerLayoutImpl
      drawerLayout.setChildInsets(insets, insets.systemWindowInsetTop > 0)
      return insets.consumeSystemWindowInsets()
    }
  }
}
