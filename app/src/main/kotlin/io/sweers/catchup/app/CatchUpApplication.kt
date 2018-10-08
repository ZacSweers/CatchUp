/*
 * Copyright (c) 2018 Zac Sweers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.sweers.catchup.app

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.SharedPreferences
import android.os.Looper
import androidx.appcompat.app.AppCompatDelegate
import com.f2prateek.rx.preferences2.RxSharedPreferences
import com.gabrielittner.threetenbp.LazyThreeTen
import com.google.firebase.perf.FirebasePerformance
import com.squareup.leakcanary.LeakCanary
import com.squareup.leakcanary.RefWatcher
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasActivityInjector
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.android.schedulers.AndroidSchedulers
import io.sweers.catchup.P
import io.sweers.catchup.data.LumberYard
import io.sweers.catchup.util.d
import javax.inject.Inject

@SuppressLint("Registered")
abstract class CatchUpApplication : Application(), HasActivityInjector {

  companion object {

    init {
      RxAndroidPlugins.setInitMainThreadSchedulerHandler {
        AndroidSchedulers.from(Looper.getMainLooper(), true)
      }
    }

    @JvmStatic
    lateinit var refWatcher: RefWatcher

    fun refWatcher() = refWatcher
  }

  @Inject
  internal lateinit var dispatchingActivityInjector: DispatchingAndroidInjector<Activity>
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
    if (LeakCanary.isInAnalyzerProcess(this)) {
      // This process is dedicated to LeakCanary for heap analysis.
      return
    }
    LazyThreeTen.init(this)
    onPreInject()
    inject()
    P.init(this, false)
    P.setSharedPreferences(sharedPreferences, rxPreferences)
    initVariant()

    P.DaynightAuto.rx()
        .asObservable()
        .subscribe { autoEnabled ->
          d { "Updating daynight" }
          // Someday would like to add activity lifecycle callbacks to automatically call recreate
          // when resumed since this was updated
          var nightMode = AppCompatDelegate.MODE_NIGHT_NO
          if (autoEnabled) {
            nightMode = AppCompatDelegate.MODE_NIGHT_AUTO
          } else if (P.DaynightNight.get()) {
            nightMode = AppCompatDelegate.MODE_NIGHT_YES
          }
          AppCompatDelegate.setDefaultNightMode(nightMode)
        }
  }

  /**
   * I give you - the only use case of method injection I've ever found.
   */
  @Inject
  protected fun initPerformanceMonitoring(sharedPreferences: SharedPreferences) {
    FirebasePerformance.getInstance().isPerformanceCollectionEnabled =
        sharedPreferences.getBoolean(P.Reports.KEY, false)
  }

  override fun activityInjector(): DispatchingAndroidInjector<Activity> =
      dispatchingActivityInjector

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
