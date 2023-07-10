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
package io.sweers.catchup.app

import android.app.Application
import android.os.StrictMode
import android.os.strictmode.DiskReadViolation
import android.os.strictmode.UntaggedSocketViolation
import dev.zacsweers.catchup.appconfig.AppConfig
import io.sweers.catchup.CatchUpPreferences
import io.sweers.catchup.app.ApplicationModule.AsyncInitializers
import io.sweers.catchup.app.ApplicationModule.Initializers
import io.sweers.catchup.injection.DaggerSet
import io.sweers.catchup.util.d
import java.util.concurrent.Executors
import javax.inject.Inject
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber

private typealias InitializerFunction = () -> @JvmSuppressWildcards Unit

class CatchUpApplication : Application() {

  @Inject internal lateinit var catchUpPreferences: CatchUpPreferences
  @Inject internal lateinit var appConfig: AppConfig

  lateinit var appComponent: ApplicationComponent

  @Inject
  internal fun plantTimberTrees(trees: DaggerSet<Timber.Tree>) {
    Timber.plant(*trees.toTypedArray())
  }

  @OptIn(DelicateCoroutinesApi::class)
  @Inject
  internal fun asyncInits(@AsyncInitializers asyncInitializers: DaggerSet<InitializerFunction>) {
    GlobalScope.launch(Dispatchers.IO) {
      // TODO - run these in parallel?
      asyncInitializers.forEach { it() }
    }
  }

  @Inject
  internal fun inits(@Initializers initializers: DaggerSet<InitializerFunction>) {
    initializers.forEach { it() }
  }

  override fun onCreate() {
    super.onCreate()
    appComponent =
      DaggerApplicationComponent.factory().create(this).apply { inject(this@CatchUpApplication) }

    StrictMode.setVmPolicy(
      StrictMode.VmPolicy.Builder()
        .detectAll()
        .penaltyListener(Executors.newSingleThreadExecutor()) { violation ->
          if (violation is UntaggedSocketViolation) {
            // This is a known issue with Flipper
          } else if (
            violation is DiskReadViolation &&
              violation.stackTraceToString().contains("CustomTabsConnection")
          ) {
            // This is a known issue with Chrome Custom Tabs
          } else {
            Timber.e(violation.toString())
          }
        }
        .build()
    )
  }

  override fun onTrimMemory(level: Int) {
    super.onTrimMemory(level)
    when (level) {
      TRIM_MEMORY_MODERATE,
      TRIM_MEMORY_RUNNING_LOW,
      TRIM_MEMORY_RUNNING_MODERATE,
      TRIM_MEMORY_BACKGROUND,
      TRIM_MEMORY_UI_HIDDEN,
      TRIM_MEMORY_COMPLETE,
      TRIM_MEMORY_RUNNING_CRITICAL -> d { "OnTrimMemory" }
    }
  }
}
