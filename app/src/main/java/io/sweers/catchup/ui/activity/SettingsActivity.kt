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

package io.sweers.catchup.ui.activity

import android.os.Bundle
import android.preference.Preference
import android.preference.PreferenceFragment
import android.preference.PreferenceScreen
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import io.sweers.catchup.P
import io.sweers.catchup.R
import io.sweers.catchup.util.clearCache
import io.sweers.catchup.util.format

class SettingsActivity : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_settings)

    if (savedInstanceState == null) {
      fragmentManager.beginTransaction()
          .add(R.id.container, SettingsFrag())
          .commit()
    }
  }

  class SettingsFrag : PreferenceFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
      addPreferencesFromResource(R.xml.prefs_general)
    }

    override fun onPreferenceTreeClick(preferenceScreen: PreferenceScreen,
        preference: Preference): Boolean {
      when (preference.key) {
        P.clearCache.key -> {
          val cleaned = activity.applicationContext.clearCache()
          Toast.makeText(
              activity,
              getString(R.string.clear_cache_success, cleaned.format()),
              Toast.LENGTH_SHORT)
              .show()
        }
        P.licenses.key -> Toast.makeText(activity, "TODO", Toast.LENGTH_SHORT)
            .show()
        P.about.key -> Toast.makeText(activity, "TODO", Toast.LENGTH_SHORT)
            .show()
      }

      return super.onPreferenceTreeClick(preferenceScreen, preference)
    }
  }
}
