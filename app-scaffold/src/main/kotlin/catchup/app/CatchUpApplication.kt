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

import android.app.Application
import android.os.StrictMode
import android.os.strictmode.DiskReadViolation
import android.os.strictmode.UntaggedSocketViolation
import android.util.Log
import catchup.app.ApplicationModule.AsyncInitializers
import catchup.app.ApplicationModule.Initializers
import catchup.app.data.DiskLumberYard
import catchup.app.data.LumberYard
import catchup.app.injection.DaggerSet
import catchup.app.util.BackgroundAppCoroutineScope
import catchup.appconfig.AppConfig
import catchup.util.d
import java.util.concurrent.Executors
import javax.inject.Inject
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import timber.log.Timber
import timber.log.Timber.Tree

private typealias InitializerFunction = () -> @JvmSuppressWildcards Unit

class CatchUpApplication : Application() {

  @Inject lateinit var appConfig: AppConfig

  lateinit var appComponent: ApplicationComponent

  @Inject
  fun plantTimberTrees(trees: DaggerSet<Tree>) {
    Timber.plant(*trees.toTypedArray())
  }

  @Inject
  fun asyncInits(
    scope: BackgroundAppCoroutineScope,
    @AsyncInitializers asyncInitializers: DaggerSet<InitializerFunction>,
  ) {
    scope.launch {
      // TODO - run these in parallel?
      asyncInitializers.forEach { it() }
    }
  }

  @Inject
  fun setupLumberYard(lumberYard: LumberYard, clock: Clock) {
    if (lumberYard !is DiskLumberYard) return
    val defaultHandler = Thread.currentThread().uncaughtExceptionHandler
    Thread.currentThread().setUncaughtExceptionHandler { thread, throwable ->
      runBlocking {
        lumberYard.addEntry(
          LumberYard.Entry(
            clock.now().toLocalDateTime(TimeZone.currentSystemDefault()),
            Log.ERROR,
            "FATAL",
            throwable.message ?: "No message",
          )
        )
        lumberYard.closeAndJoin()
      }
      defaultHandler?.uncaughtException(thread, throwable)
    }
  }

  @Inject
  fun inits(@Initializers initializers: DaggerSet<InitializerFunction>) {
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

  @Suppress("DEPRECATION")
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
