/*
 * Copyright (C) 2022 Slack Technologies, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package catchup.app.di

import android.app.Activity
import android.app.Application
import android.content.Intent
import androidx.annotation.Keep
import androidx.core.app.AppComponentFactory
import catchup.app.CatchUpApplication
import dev.zacsweers.catchup.app.scaffold.BuildConfig
import javax.inject.Provider
import kotlinx.coroutines.DEBUG_PROPERTY_NAME

@Keep
class CatchUpAppComponentFactory : AppComponentFactory() {

  private inline fun <reified T> getInstance(
    cl: ClassLoader,
    className: String,
    providers: Map<Class<out T>, @JvmSuppressWildcards Provider<T>>,
  ): T? {
    val clazz = Class.forName(className, false, cl).asSubclass(T::class.java)
    val modelProvider = providers[clazz] ?: return null
    return modelProvider.get() as T
  }

  override fun instantiateActivityCompat(
    cl: ClassLoader,
    className: String,
    intent: Intent?,
  ): Activity {
    return getInstance(cl, className, applicationRef.appComponent.activityProviders)
      ?: super.instantiateActivityCompat(cl, className, intent)
  }

  override fun instantiateApplicationCompat(cl: ClassLoader, className: String): Application {
    val app = super.instantiateApplicationCompat(cl, className)
    applicationRef = (app as CatchUpApplication)
    return app
  }

  // AppComponentFactory can be created multiple times
  companion object {
    private lateinit var applicationRef: CatchUpApplication

    // Enable coroutines debug mode in debug builds
    init {
      if (BuildConfig.DEBUG) {
        System.setProperty(DEBUG_PROPERTY_NAME, "on")
        System.setProperty("kotlinx.coroutines.stacktrace.recovery", "true")
      }
    }
  }
}
