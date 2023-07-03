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
package io.sweers.catchup

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
import com.squareup.anvil.annotations.ContributesBinding
import dev.zacsweers.catchup.di.AppScope
import dev.zacsweers.catchup.di.SingleIn
import io.sweers.catchup.CatchUpPreferences.Keys
import io.sweers.catchup.util.injection.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface CatchUpPreferences {
  val datastore: DataStore<Preferences>
  val dayNightAuto: Flow<Boolean>
  val dayNightForceNight: Flow<Boolean>
  val dynamicTheme: Flow<Boolean>
  val reports: Flow<Boolean>
  val servicesOrder: Flow<ImmutableList<String>>
  val servicesOrderSeen: Flow<Boolean>
  val smartlinkingGlobal: Flow<Boolean>
  val lastVersion: Flow<String?>

  suspend fun edit(body: (MutablePreferences) -> Unit)

  object Keys {
    val dayNightAuto = booleanPreferencesKey("dayNightAuto")
    val dayNightForceNight = booleanPreferencesKey("forceNight")
    val dynamicTheme = booleanPreferencesKey("dynamicTheme")
    val reports = booleanPreferencesKey("reports")
    val smartlinkingGlobal = booleanPreferencesKey("smartLinkingGlobal")
    val servicesOrderSeen = booleanPreferencesKey("servicesOrderSeen")
    val servicesOrder = stringPreferencesKey("servicesOrder")
    val lastVersion = stringPreferencesKey("lastVersion")
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
class CatchUpPreferencesImpl @Inject constructor(@ApplicationContext context: Context) :
  CatchUpPreferences {

  // TODO hide this, only exposed for settings
  override val datastore =
    PreferenceDataStoreFactory.create { CatchUpPreferences.dataStoreFile(context) }

  override val dayNightAuto: Flow<Boolean>
    get() = datastore.data.map { it[Keys.dayNightAuto] ?: true }

  override val dayNightForceNight: Flow<Boolean>
    get() = datastore.data.map { it[Keys.dayNightForceNight] ?: false }

  override val dynamicTheme: Flow<Boolean>
    get() = datastore.data.map { it[Keys.dynamicTheme] ?: false }

  override val reports: Flow<Boolean>
    get() = datastore.data.map { it[Keys.reports] ?: true }

  override val servicesOrder: Flow<ImmutableList<String>>
    get() = datastore.data.map { it[Keys.servicesOrder]?.split(',').orEmpty().toImmutableList() }

  override val servicesOrderSeen: Flow<Boolean>
    get() = datastore.data.map { it[Keys.servicesOrderSeen] ?: false }

  override val smartlinkingGlobal: Flow<Boolean>
    get() = datastore.data.map { it[Keys.smartlinkingGlobal] ?: true }

  override val lastVersion: Flow<String?>
    get() = datastore.data.map { it[Keys.lastVersion] }

  override suspend fun edit(body: (MutablePreferences) -> Unit) {
    datastore.edit(body)
  }
}
