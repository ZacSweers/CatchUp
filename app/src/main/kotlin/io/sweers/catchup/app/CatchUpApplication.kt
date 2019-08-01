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

import android.annotation.SuppressLint
import android.app.Application
import android.content.SharedPreferences
import android.os.Looper
import androidx.appcompat.app.AppCompatDelegate
import com.chibatching.kotpref.Kotpref
import com.f2prateek.rx.preferences2.RxSharedPreferences
import com.gabrielittner.threetenbp.LazyThreeTen
import com.uber.rxdogtag.RxDogTag
import com.uber.rxdogtag.autodispose.AutoDisposeConfigurer
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.android.schedulers.AndroidSchedulers
import io.sweers.catchup.CatchUpPreferences
import io.sweers.catchup.data.LumberYard
import io.sweers.catchup.flowFor
import io.sweers.catchup.util.d
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@SuppressLint("Registered")
abstract class CatchUpApplication : Application(), HasAndroidInjector {

  companion object {

    init {
      RxAndroidPlugins.setInitMainThreadSchedulerHandler {
        AndroidSchedulers.from(Looper.getMainLooper(), true)
      }
      RxDogTag.builder()
          .configureWith(AutoDisposeConfigurer::configure)
          .install()
    }

    @JvmStatic
    internal lateinit var objectWatcher: CatchUpObjectWatcher

    fun refWatcher() = objectWatcher
  }

  @Inject
  internal lateinit var androidInjector: DispatchingAndroidInjector<Any>
  @Inject
  internal lateinit var sharedPreferences: SharedPreferences
  @Inject
  internal lateinit var lumberYard: LumberYard
  @Inject
  internal lateinit var rxPreferences: RxSharedPreferences

  open fun onPreInject() {
  }

  abstract fun inject()

  // Override this in variants
  protected open fun initVariant() = Unit

  override fun onCreate() {
    super.onCreate()
    GlobalScope.launch {
      // This makes a call to disk, so initialize it off the main thread first... ironically
      Dispatchers.Main
    }
    LazyThreeTen.init(this)
    onPreInject()
    inject()
    Kotpref.init(this)
    initVariant()

    CatchUpPreferences.flowFor { ::daynightAuto }
        .onEach { autoEnabled ->
            d { "Updating daynight" }
            // Someday would like to add activity lifecycle callbacks to automatically call recreate
            // when resumed since this was updated
            var nightMode = AppCompatDelegate.MODE_NIGHT_NO
            if (autoEnabled) {
              nightMode = AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY
            } else if (CatchUpPreferences.dayNight) {
              nightMode = AppCompatDelegate.MODE_NIGHT_YES
            }
            AppCompatDelegate.setDefaultNightMode(nightMode)
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

interface CatchUpObjectWatcher {
  fun watch(watchedReference: Any)

  object None : CatchUpObjectWatcher {
    override fun watch(watchedReference: Any) {
    }
  }
}
