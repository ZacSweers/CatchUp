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
import javax.inject.Inject
import javax.inject.Singleton
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
  val property = propertyResolver()
  return callbackFlow<T> {
    val prefsListener = object : SharedPreferences.OnSharedPreferenceChangeListener {
      private val key: String = property.name

      override fun onSharedPreferenceChanged(prefs: SharedPreferences, propertyName: String) {
        if (propertyName == key) {
          safeOffer(property.get())
        }
      }
    }
    safeOffer(property.get())
    preferences.registerOnSharedPreferenceChangeListener(prefsListener)
    awaitClose {
      preferences.unregisterOnSharedPreferenceChangeListener(prefsListener)
    }
  }
}
