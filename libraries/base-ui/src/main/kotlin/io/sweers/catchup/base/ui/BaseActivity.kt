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

import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NavUtils
import dev.zacsweers.catchup.appconfig.AppConfig

abstract class BaseActivity : AppCompatActivity() {

  abstract val appConfig: AppConfig

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    if (item.itemId == android.R.id.home) {
      NavUtils.navigateUpFromSameTask(this)
    }
    return super.onOptionsItemSelected(item)
  }

  @Deprecated("Deprecated in Java")
  override fun onBackPressed() {
    supportFragmentManager.fragments.filterIsInstance<BackpressHandler>().forEach {
      if (it.onBackPressed()) {
        return
      }
    }
    if (appConfig.sdkInt == 29 && isTaskRoot) {
      // https://twitter.com/Piwai/status/1169274622614704129
      // https://issuetracker.google.com/issues/139738913
      finishAfterTransition()
    } else {
      super.onBackPressed()
    }
  }
}
