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
import android.os.Looper
import androidx.appcompat.app.AppCompatDelegate
import com.f2prateek.rx.preferences2.RxSharedPreferences
import com.uber.rxdogtag.RxDogTag
import com.uber.rxdogtag.autodispose.AutoDisposeConfigurer
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.android.schedulers.AndroidSchedulers
import io.sweers.catchup.CatchUpPreferences
import io.sweers.catchup.app.ApplicationModule.AsyncInitializers
import io.sweers.catchup.app.ApplicationModule.Initializers
import io.sweers.catchup.flowFor
import io.sweers.catchup.util.d
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

private typealias InitializerFunction = () -> @JvmSuppressWildcards Unit

class CatchUpApplication : Application(), HasAndroidInjector {

  companion object {

    init {
      RxAndroidPlugins.setInitMainThreadSchedulerHandler {
        AndroidSchedulers.from(Looper.getMainLooper(), true)
      }
      RxDogTag.builder()
          .configureWith(AutoDisposeConfigurer::configure)
          .install()
    }

    internal lateinit var appComponent: ApplicationComponent
  }

  @Inject
  internal lateinit var androidInjector: DispatchingAndroidInjector<Any>
  @Inject
  internal lateinit var catchUpPreferences: CatchUpPreferences
  @Inject
  internal lateinit var rxPreferences: RxSharedPreferences

  @Inject
  internal fun plantTimberTrees(trees: Set<@JvmSuppressWildcards Timber.Tree>) {
    Timber.plant(*trees.toTypedArray())
  }

  @Inject
  internal fun asyncInits(@AsyncInitializers asyncInitializers: Set<@JvmSuppressWildcards InitializerFunction>) {
    GlobalScope.launch(Dispatchers.IO) {
      // TODO - run these in parallel?
      asyncInitializers.forEach { it() }
    }
  }

  @Inject
  internal fun inits(@Initializers initializers: Set<@JvmSuppressWildcards InitializerFunction>) {
    initializers.forEach { it() }
  }

  override fun onCreate() {
    super.onCreate()
    appComponent = inject()

    GlobalScope.launch {
      catchUpPreferences.flowFor { ::daynightAuto }
          .collect { autoEnabled ->
            d { "Updating daynight" }
            // Someday would like to add activity lifecycle callbacks to automatically call recreate
            // when resumed since this was updated
            var nightMode = AppCompatDelegate.MODE_NIGHT_NO
            if (autoEnabled) {
              nightMode = AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY
            } else if (catchUpPreferences.dayNight) {
              nightMode = AppCompatDelegate.MODE_NIGHT_YES
            }
            AppCompatDelegate.setDefaultNightMode(nightMode)
          }
    }
  }

  override fun androidInjector(): DispatchingAndroidInjector<Any> =
      androidInjector

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
