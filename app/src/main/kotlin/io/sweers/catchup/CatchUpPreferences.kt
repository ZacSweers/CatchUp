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
import android.content.SharedPreferences
import androidx.annotation.Keep
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import com.chibatching.kotpref.KotprefModel
import com.squareup.anvil.annotations.ContributesBinding
import dev.zacsweers.catchup.di.AppScope
import dev.zacsweers.catchup.di.SingleIn
import io.sweers.catchup.CatchUpPreferences.Keys
import io.sweers.catchup.base.ui.UiPreferences
import io.sweers.catchup.flowbinding.safeOffer
import io.sweers.catchup.util.injection.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlin.LazyThreadSafetyMode.NONE
import kotlin.reflect.KProperty0
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
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

fun <T, Model : KotprefModel> Model.flowFor(propertyResolver: Model.() -> KProperty0<T>): Flow<T> {
  val lazilyResolved by lazy(NONE) { propertyResolver() }
  return preferences.flowFor(
    keyResolver = {
      // With kotlin-reflect, we could get the key declared on the delegate
      //        (lazilyResolved.getDelegate() as PreferenceKey).key ?: lazilyResolved.name
      lazilyResolved.name
    },
    valueResolver = {
      println("ORDER getting value for ${lazilyResolved.name}")
      lazilyResolved.get()
    }
  )
}

fun <T> SharedPreferences.flowFor(keyResolver: () -> String, valueResolver: () -> T): Flow<T> {
  return flowFor(keyResolver).map { valueResolver() }
}

fun SharedPreferences.flowFor(keyResolver: () -> String): Flow<Unit> {
  return callbackFlow {
    val targetKey = keyResolver()
    val prefsListener =
      SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        // TODO this is called twice when updating, say, a switch bound to this. Need to find a way
        //  to handle reentrant cases
        if (key == targetKey) {
          safeOffer(Unit)
        }
      }
    registerOnSharedPreferenceChangeListener(prefsListener)
    awaitClose { unregisterOnSharedPreferenceChangeListener(prefsListener) }
  }
}
