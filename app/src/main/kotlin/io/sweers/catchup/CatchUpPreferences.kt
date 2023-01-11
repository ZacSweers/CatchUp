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
import io.sweers.catchup.base.ui.UiPreferences
import io.sweers.catchup.util.injection.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull

interface CatchUpPreferences : UiPreferences {
  val datastore: DataStore<Preferences>
  val dayNightAuto: Flow<Boolean>
  val dayNightForceNight: Flow<Boolean>
  val reports: Flow<Boolean>
  val servicesOrder: Flow<String?>
  val servicesOrderSeen: Flow<Boolean>
  val smartlinkingGlobal: Flow<Boolean>
  override var themeNavigationBar: Boolean

  suspend fun edit(body: (MutablePreferences) -> Unit)

  object Keys {
    val dayNightAuto = booleanPreferencesKey("dayNightAuto")
    val dayNightForceNight = booleanPreferencesKey("forceNight")
    val reports = booleanPreferencesKey("reports")
    val smartlinkingGlobal = booleanPreferencesKey("smartLinkingGlobal")
    val servicesOrderSeen = booleanPreferencesKey("servicesOrderSeen")
    val servicesOrder = stringPreferencesKey("servicesOrder")
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
  CatchUpPreferences, UiPreferences {

  // TODO hide this, only exposed for settings
  override val datastore =
    PreferenceDataStoreFactory.create { CatchUpPreferences.dataStoreFile(context) }

  // TODO inline default values here
  override val dayNightAuto: Flow<Boolean>
    get() = datastore.data.mapNotNull { it[Keys.dayNightAuto] }
  override val dayNightForceNight: Flow<Boolean>
    get() = datastore.data.mapNotNull { it[Keys.dayNightForceNight] }
  override val reports: Flow<Boolean>
    get() = datastore.data.mapNotNull { it[Keys.reports] }
  override val servicesOrder: Flow<String?>
    get() = datastore.data.map { it[Keys.servicesOrder] }
  override val servicesOrderSeen: Flow<Boolean>
    get() = datastore.data.mapNotNull { it[Keys.servicesOrderSeen] }
  override val smartlinkingGlobal: Flow<Boolean>
    get() = datastore.data.mapNotNull { it[Keys.smartlinkingGlobal] }

  override var themeNavigationBar = false

  override suspend fun edit(body: (MutablePreferences) -> Unit) {
    datastore.edit(body)
  }
}
