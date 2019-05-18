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
package io.sweers.catchup.app

import android.util.Log
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Error
import timber.log.Timber
import java.util.ArrayDeque
import java.util.Locale

/**
 * A logging implementation which buffers the last 200 messages and notifies on error exceptions.
 */
internal class BugsnagTree : Timber.Tree() {

  companion object {
    private const val BUFFER_SIZE = 200

    private fun priorityToString(priority: Int) = when (priority) {
      Log.ERROR -> "E"
      Log.WARN -> "W"
      Log.INFO -> "I"
      Log.DEBUG -> "D"
      else -> priority.toString()
    }
  }

  // Adding one to the initial size accounts for the add before remove.
  private val buffer = ArrayDeque<String>(BUFFER_SIZE + 1)

  override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
    val adjustedMessage = """${System.currentTimeMillis()} ${priorityToString(priority)} $message"""
    synchronized(buffer) {
      buffer.addLast(adjustedMessage)
      if (buffer.size > BUFFER_SIZE) {
        buffer.removeFirst()
      }
    }
    if (t != null && priority == Log.ERROR) {
      Bugsnag.notify(t)
    }
  }

  fun update(error: Error) {
    synchronized(buffer) {
      var i = 1
      for (message in buffer) {
        error.addToTab("Log", String.format(Locale.US, "%03d", i++), message)
      }
    }
  }
}
