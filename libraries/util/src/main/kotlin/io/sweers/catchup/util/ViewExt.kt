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
@file:Suppress("NOTHING_TO_INLINE")

package io.sweers.catchup.util

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.view.View
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator

fun View.setLightStatusBar() {
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
    if ((View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR and systemUiVisibility) != View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR) {
      var flags = systemUiVisibility
      flags = flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
      systemUiVisibility = flags
    }
  }
}

fun View.clearLightStatusBar() {
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
    var flags = systemUiVisibility
    // TODO noop if it's already not set. My bitwise-fu is not strong enough to figure out the inverse of setting it
    flags = flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
    systemUiVisibility = flags
  }
}

fun View.setLightNavBar() {
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    if ((View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR and systemUiVisibility) != View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR) {
      var flags = systemUiVisibility
      flags = flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
      systemUiVisibility = flags
    }
  }
}

fun View.clearLightNavBar() {
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    var flags = systemUiVisibility
    // TODO noop if it's already not set. My bitwise-fu is not strong enough to figure out the inverse of setting it
    flags = flags and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
    systemUiVisibility = flags
  }
}

inline fun View.show(animate: Boolean = false) {
  if (isVisible) {
    return
  }
  if (animate) {
    alpha = 0F
    visibility = View.VISIBLE
    animate()
        .setDuration(300)
        .setInterpolator(FastOutSlowInInterpolator())
        .withLayer()
        .alpha(1F)
        .setListener(object : AnimatorListenerAdapter() {
          override fun onAnimationEnd(animation: Animator) {
            animation.removeAllListeners()
          }
        })
  } else {
    visibility = View.VISIBLE
  }
}

inline infix fun View.showIf(condition: Boolean) {
  if (condition) {
    show()
  } else {
    hide()
  }
}

inline fun View.hide(animate: Boolean = false) {
  if (!isVisible) {
    return
  }
  if (animate && !isInvisible) {
    animate()
        .setDuration(300)
        .setInterpolator(LinearOutSlowInInterpolator())
        .withLayer()
        .alpha(0F)
        .setListener(object : AnimatorListenerAdapter() {
          override fun onAnimationEnd(animation: Animator) {
            animation.removeAllListeners()
            visibility = View.GONE
            alpha = 1F
          }
        })
  } else {
    visibility = View.GONE
  }
}

inline infix fun View.hideIf(condition: Boolean) {
  if (condition) {
    hide()
  } else {
    show()
  }
}

inline fun View.toggleVisibility(animate: Boolean = false) {
  if (isVisible) {
    hide(animate)
  } else {
    show(animate)
  }
}

fun Context.asDayContext(): Context {
  return if (isInNightMode()) {
    createConfigurationContext(
        Configuration(resources.configuration)
            .apply { uiMode = Configuration.UI_MODE_NIGHT_NO })
  } else this
}
