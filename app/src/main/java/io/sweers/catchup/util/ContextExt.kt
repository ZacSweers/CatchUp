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

package io.sweers.catchup.util

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.support.annotation.AttrRes
import android.support.annotation.ColorInt
import android.support.annotation.UiThread
import android.util.TypedValue
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import io.sweers.catchup.R.string
import java.io.File

fun Context.clearCache(): Long {
  return cleanDir(applicationContext.cacheDir)
}

private fun cleanDir(dir: File): Long {
  var bytesDeleted: Long = 0
  val files = dir.listFiles()

  for (file in files) {
    val length = file.length()
    if (file.delete()) {
      d { "Deleted file" }
      bytesDeleted += length
    }
  }
  return bytesDeleted
}

/**
 * Attempt to launch the supplied [Intent]. Queries on-device packages before launching and
 * will display a simple message if none are available to handle it.
 */
fun Context.maybeStartActivity(
    intent: Intent): Boolean = maybeStartActivity(intent, false)

/**
 * Attempt to launch Android's chooser for the supplied [Intent]. Queries on-device
 * packages before launching and will display a simple message if none are available to handle
 * it.
 */
fun Context.maybeStartChooser(
    intent: Intent): Boolean = maybeStartActivity(intent, true)

private fun Context.maybeStartActivity(inputIntent: Intent,
    chooser: Boolean): Boolean {
  var intent = inputIntent
  if (hasHandler(intent)) {
    if (chooser) {
      intent = Intent.createChooser(intent, null)
    }
    startActivity(intent)
    return true
  } else {
    Toast.makeText(this, string.no_intent_handler,
        LENGTH_LONG).show()
    return false
  }
}

/**
 * Queries on-device packages for a handler for the supplied [Intent].
 */
private fun Context.hasHandler(intent: Intent): Boolean {
  return !packageManager.queryIntentActivities(intent, 0).isEmpty()
}

private val TYPED_VALUE = TypedValue()

@ColorInt
@UiThread
fun Context.resolveAttribute(@AttrRes resId: Int): Int {
  val theme = theme
  theme.resolveAttribute(resId, TYPED_VALUE, true)
  @ColorInt val color = TYPED_VALUE.data
  return color
}

fun Context.isInNightMode(): Boolean {
  val conf = resources
      .configuration
  return conf.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
}

/**
 * Determine if the navigation bar will be on the bottom of the screen, based on logic in
 * PhoneWindowManager.
 */
fun Context.isNavBarOnBottom(): Boolean {
  val res = resources
  val cfg = resources
      .configuration
  val dm = res.displayMetrics
  val canMove = dm.widthPixels != dm.heightPixels && cfg.smallestScreenWidthDp < 600
  return !canMove || dm.widthPixels < dm.heightPixels
}
