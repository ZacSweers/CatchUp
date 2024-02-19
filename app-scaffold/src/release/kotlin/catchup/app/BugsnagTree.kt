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
package catchup.app

import android.util.Log
import catchup.app.data.LumberYard
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Event
import java.util.Locale
import timber.log.Timber

/**
 * A logging implementation which buffers the last 200 messages and notifies on error exceptions.
 */
internal class BugsnagTree(private val lumberYard: LumberYard) : Timber.Tree() {

  override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
    if (t != null && priority == Log.ERROR) {
      Bugsnag.notify(t)
    }
  }

  fun update(event: Event) {
    for ((i, entry) in lumberYard.bufferedLogs().withIndex()) {
      val message = entry.prettyPrint()
      event.addMetadata("Log", String.format(Locale.US, "%03d", i), message)
    }
  }
}
