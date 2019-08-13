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
package io.sweers.catchup.data

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import com.chibatching.kotpref.ContextProvider
import com.chibatching.kotpref.KotprefModel
import com.chibatching.kotpref.PreferencesOpener
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DebugPreferences @Inject constructor(
  application: Application,
  sharedPreferences: SharedPreferences
) : KotprefModel(
    contextProvider = object : ContextProvider {
      override fun getApplicationContext(): Context = application
    },
    opener = object : PreferencesOpener {
      override fun openPreferences(context: Context, name: String, mode: Int): SharedPreferences {
        return sharedPreferences
      }
    }
) {
  var animationSpeed by intPref(default = 1, key = "debug_animation_speed")
  var mockModeEnabled by booleanPref(default = false, key = "debug_mock_mode_enabled")
  var networkDelay by longPref(default = 2000L, key = "debug_network_delay")
  var networkFailurePercent by intPref(default = 3, key = "debug_network_failure_percent")
  var networkVariancePercent by intPref(default = 40, key = "debug_network_variance_percent")
  var pixelGridEnabled by booleanPref(default = false, key = "debug_pixel_grid_enabled")
  var pixelRatioEnabled by booleanPref(default = false, key = "debug_pixel_ratio_enabled")
  var scalpelEnabled by booleanPref(default = false, key = "debug_scalpel_enabled")
  var scalpelWireframeDrawer by booleanPref(default = false, key = "debug_scalpel_wireframe_drawer")
  var seenDebugDrawer by booleanPref(default = false, key = "debug_seen_debug_drawer")
}
