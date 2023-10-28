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
import android.content.Intent
import android.content.res.Configuration
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import java.io.File
import java.io.IOException
import java.util.Locale

/*
 * Android framework extension functions for things like Context, Activity, Resources, etc
 */

fun Context.clearFiles() = cleanDir(applicationContext.filesDir)

fun Context.clearCache() = cleanDir(applicationContext.cacheDir)

fun cleanDir(dir: File): Long {
  var bytesDeleted: Long = 0
  dir.listFiles()?.forEach {
    val length = it.length()
    try {
      if (it.delete()) {
        d { "Deleted file" }
        bytesDeleted += length
      }
    } catch (e: IOException) {
      // Ignore these for now
    }
  }
  return bytesDeleted
}

/**
 * Attempt to launch the supplied [Intent]. Queries on-device packages before launching and will
 * display a simple message if none are available to handle it.
 */
fun Context.maybeStartActivity(intent: Intent): Boolean = maybeStartActivity(intent, false)

private fun Context.maybeStartActivity(inputIntent: Intent, chooser: Boolean): Boolean {
  var intent = inputIntent
  return if (hasHandler(intent)) {
    if (chooser) {
      intent = Intent.createChooser(intent, null)
    }
    startActivity(intent)
    true
  } else {
    Toast.makeText(this, R.string.catchup_util_no_intent_handler, LENGTH_LONG).show()
    false
  }
}

/** Queries on-device packages for a handler for the supplied [Intent]. */
private fun Context.hasHandler(intent: Intent) =
  packageManager.queryIntentActivities(intent, 0).isNotEmpty()

fun Context.isInNightMode(): Boolean {
  return when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
    Configuration.UI_MODE_NIGHT_NO -> false // Night mode is not active, we're in day time
    Configuration.UI_MODE_NIGHT_YES -> true // Night mode is active, we're at night!
    else -> false // We don't know what mode we're in, assume not night
  }
}

val Context.primaryLocale: Locale
  get() {
    return resources.configuration.locales[0]
  }
