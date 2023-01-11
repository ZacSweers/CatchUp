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
package io.sweers.catchup.util

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.activity.ComponentActivity
import com.jakewharton.processphoenix.ProcessPhoenix
import io.sweers.catchup.ui.activity.LauncherActivity

fun Context.resolveActivity(): ComponentActivity {
  if (this is ComponentActivity) {
    return this
  }
  if (this is ContextWrapper) {
    return baseContext.resolveActivity()
  }
  throw UnsupportedOperationException("Given context was not an activity! Is a $this")
}

fun Context.restartApp() {
  ProcessPhoenix.triggerRebirth(this, Intent(this, LauncherActivity::class.java))
}
