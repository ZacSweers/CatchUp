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

import android.content.SharedPreferences
import androidx.annotation.Keep
import com.chibatching.kotpref.KotprefModel
import io.sweers.catchup.flowbinding.safeOffer
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.reflect.KProperty0

@Keep
object CatchUpPreferences : KotprefModel() {

  var daynightAuto by booleanPref(default = true)
  var dayNight by booleanPref(default = false)
  var dayNightForceNight by booleanPref(default = false)
  var reports by booleanPref(default = true)
  var servicesOrderSeen by booleanPref(default = false)
  var smartlinkingGlobal by booleanPref(default = true)
  var themeNavigationBar by booleanPref(default = false)

  const val about = "about"
  const val clearCache = "clear_cache"
  const val reorderServicesSection = "reorder_services_section"
  var services = "services"
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
