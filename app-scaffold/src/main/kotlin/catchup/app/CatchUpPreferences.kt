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

import android.content.Context
import androidx.annotation.Keep
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import catchup.app.CatchUpPreferences.Keys
import catchup.app.util.BackgroundAppCoroutineScope
import catchup.di.AppScope
import catchup.di.DataMode
import catchup.di.SingleIn
import catchup.util.injection.qualifiers.ApplicationContext
import com.squareup.anvil.annotations.ContributesBinding
import java.io.File
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

interface BasePreferences {
  val datastore: DataStore<Preferences>
  val scope: CoroutineScope
  val initialValues: Preferences

  fun <KeyType> preferenceStateFlow(
    key: Preferences.Key<KeyType>,
    defaultValue: KeyType,
  ): StateFlow<KeyType> {
    return preferenceStateFlow(key, defaultValue) { it }
  }

  fun <KeyType, StateType> preferenceStateFlow(
    key: Preferences.Key<KeyType>,
    defaultValue: StateType,
    transform: ((KeyType) -> StateType?),
  ): StateFlow<StateType> {
    val initialValue = initialValues[key]?.let(transform) ?: defaultValue
    val stateFlow = MutableStateFlow(initialValue)
    scope.launch {
      datastore.data
        .map { preferences -> preferences[key]?.let(transform) ?: defaultValue }
        .collect(stateFlow::emit)
    }
    return stateFlow
  }
}

interface CatchUpPreferences {
  val datastore: DataStore<Preferences>
  val dayNightAuto: StateFlow<Boolean>
  val dayNightForceNight: StateFlow<Boolean>
  val dynamicTheme: StateFlow<Boolean>
  val reports: StateFlow<Boolean>
  val servicesOrder: StateFlow<ImmutableList<String>?>
  val servicesOrderSeen: StateFlow<Boolean>
  val smartlinkingGlobal: StateFlow<Boolean>
  val lastVersion: StateFlow<String?>
  val dataMode: StateFlow<DataMode>

  suspend fun edit(body: (MutablePreferences) -> Unit) {
    datastore.edit(body)
  }

  object Keys {
    val dayNightAuto = booleanPreferencesKey("dayNightAuto")
    val dayNightForceNight = booleanPreferencesKey("forceNight")
    val dynamicTheme = booleanPreferencesKey("dynamicTheme")
    val reports = booleanPreferencesKey("reports")
    val smartlinkingGlobal = booleanPreferencesKey("smartLinkingGlobal")
    val servicesOrderSeen = booleanPreferencesKey("servicesOrderSeen")
    val servicesOrder = stringPreferencesKey("servicesOrder")
    val lastVersion = stringPreferencesKey("lastVersion")
    val dataMode = stringPreferencesKey("dataMode")
  }

  companion object {
    private const val STORAGE_FILE_NAME = "CatchUpPreferences"

    fun dataStoreFile(context: Context): File {
      return context.preferencesDataStoreFile(STORAGE_FILE_NAME)
    }
  }
}

@Keep
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, boundType = CatchUpPreferences::class)
class CatchUpPreferencesImpl
@Inject
constructor(@ApplicationContext context: Context, override val scope: BackgroundAppCoroutineScope) :
  CatchUpPreferences, BasePreferences {

  // TODO hide this, only exposed for settings
  override val datastore =
    PreferenceDataStoreFactory.create { CatchUpPreferences.dataStoreFile(context) }

  // TODO this is... ugly, but needed to force initial values to load from the store
  override val initialValues = runBlocking { datastore.data.first() }

  override val dayNightAuto: StateFlow<Boolean> = preferenceStateFlow(Keys.dayNightAuto, true)

  override val dayNightForceNight: StateFlow<Boolean> =
    preferenceStateFlow(Keys.dayNightForceNight, false)

  override val dynamicTheme: StateFlow<Boolean> = preferenceStateFlow(Keys.dynamicTheme, false)

  override val reports: StateFlow<Boolean> = preferenceStateFlow(Keys.reports, true)

  override val servicesOrder: StateFlow<ImmutableList<String>?> =
    preferenceStateFlow(Keys.servicesOrder, null) { it.split(',').toImmutableList() }

  override val servicesOrderSeen: StateFlow<Boolean> =
    preferenceStateFlow(Keys.servicesOrderSeen, false)

  override val smartlinkingGlobal: StateFlow<Boolean> =
    preferenceStateFlow(Keys.smartlinkingGlobal, true)

  override val lastVersion: StateFlow<String?> = preferenceStateFlow(Keys.lastVersion, null) { it }

  override val dataMode: StateFlow<DataMode> =
    preferenceStateFlow(Keys.dataMode, DataMode.REAL) { prefValue ->
      DataMode.entries.find { mode -> mode.name == prefValue }
    }
}
