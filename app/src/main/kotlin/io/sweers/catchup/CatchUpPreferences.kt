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

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.Keep
import com.chibatching.kotpref.ContextProvider
import com.chibatching.kotpref.KotprefModel
import com.chibatching.kotpref.PreferencesOpener
import io.sweers.catchup.base.ui.UiPreferences
import io.sweers.catchup.flowbinding.safeOffer
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.LazyThreadSafetyMode.NONE
import kotlin.reflect.KProperty0

@Keep
@Singleton
class CatchUpPreferences @Inject constructor(
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
), UiPreferences {

  companion object {
    const val ITEM_KEY_ABOUT = "about"
    const val ITEM_KEY_CLEAR_CACHE = "clearCache"
    const val SECTION_KEY_ORDER_SERVICES = "reorderServicesSection"
    const val SECTION_KEY_SERVICES = "services"
  }

  var daynightAuto by booleanPref(default = true)
  var dayNight by booleanPref(default = false)
  var dayNightForceNight by booleanPref(default = false)
  var reports by booleanPref(default = true)
  var servicesOrderSeen by booleanPref(default = false)
  var smartlinkingGlobal by booleanPref(default = true)
  override var themeNavigationBar by booleanPref(default = false)
  var servicesOrder by nullableStringPref(default = null)
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
        lazilyResolved.get()
      }
  )
}

fun <T> SharedPreferences.flowFor(keyResolver: () -> String, valueResolver: () -> T): Flow<T> {
  return flowFor(keyResolver)
      .map { valueResolver() }
}

fun SharedPreferences.flowFor(targetKey: String): Flow<Unit> {
  return flowFor { targetKey }
}

fun SharedPreferences.flowFor(keyResolver: () -> String): Flow<Unit> {
  return callbackFlow<Unit> {
    val targetKey = keyResolver()
    val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
      // TODO this is called twice when updating, say, a switch bound to this. Need to find a way
      //  to handle reentrant cases
      if (key == targetKey) {
        safeOffer(Unit)
      }
    }
    registerOnSharedPreferenceChangeListener(prefsListener)
    awaitClose {
      unregisterOnSharedPreferenceChangeListener(prefsListener)
    }
  }
}
