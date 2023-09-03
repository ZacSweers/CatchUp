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
package catchup.app.data

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import catchup.di.AppScope
import catchup.di.SingleIn
import catchup.util.injection.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@SingleIn(AppScope::class)
class DebugPreferences @Inject constructor(@ApplicationContext context: Context) {
  companion object {
    private const val STORAGE_FILE_NAME = "debug_preferences"
  }

  internal val datastore =
    PreferenceDataStoreFactory.create { context.preferencesDataStoreFile(STORAGE_FILE_NAME) }

  val animationSpeed: Flow<Int> = datastore.data.map { it[Keys.animationSpeed] ?: 1 }
  val mockModeEnabled: Flow<Boolean> = datastore.data.map { it[Keys.mockModeEnabled] ?: false }
  val networkDelay: Flow<Long> = datastore.data.map { it[Keys.networkDelay] ?: 2000L }
  val networkFailurePercent: Flow<Int> = datastore.data.map { it[Keys.networkFailurePercent] ?: 3 }
  val networkVariancePercent: Flow<Int> =
    datastore.data.map { it[Keys.networkVariancePercent] ?: 40 }
  val seenDebugDrawer: Flow<Boolean> = datastore.data.map { it[Keys.seenDebugDrawer] ?: false }

  suspend fun edit(body: (MutablePreferences) -> Unit) {
    datastore.edit(body)
  }

  object Keys {
    var animationSpeed = intPreferencesKey("debug_animation_speed")
    var mockModeEnabled = booleanPreferencesKey("debug_mock_mode_enabled")
    var networkDelay = longPreferencesKey("debug_network_delay")
    var networkFailurePercent = intPreferencesKey("debug_network_failure_percent")
    var networkVariancePercent = intPreferencesKey("debug_network_variance_percent")
    var seenDebugDrawer = booleanPreferencesKey("debug_seen_debug_drawer")
  }
}
