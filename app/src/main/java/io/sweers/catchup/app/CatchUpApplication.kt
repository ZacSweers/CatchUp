/*
 * Copyright (c) 2017 Zac Sweers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.sweers.catchup.app

import android.app.Activity
import android.app.Application
import android.content.SharedPreferences
import android.support.v7.app.AppCompatDelegate
import com.bumptech.glide.Glide
import com.f2prateek.rx.preferences2.RxSharedPreferences
import com.gabrielittner.threetenbp.LazyThreeTen
import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.squareup.leakcanary.LeakCanary
import com.squareup.leakcanary.RefWatcher
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasActivityInjector
import io.sweers.catchup.P
import io.sweers.catchup.R
import io.sweers.catchup.data.LumberYard
import io.sweers.catchup.util.d
import javax.inject.Inject

open class CatchUpApplication : Application(), HasActivityInjector {

  companion object {

    @JvmStatic lateinit var refWatcher: RefWatcher

    fun refWatcher(): RefWatcher {
      return refWatcher
    }
  }

  @Inject lateinit internal var dispatchingActivityInjector: DispatchingAndroidInjector<Activity>
  @Inject lateinit var sharedPreferences: SharedPreferences
  @Inject lateinit var lumberYard: LumberYard
  @Inject lateinit var remoteConfig: FirebaseRemoteConfig
  @Inject lateinit var rxPreferences: RxSharedPreferences

  override fun onCreate() {
    super.onCreate()
    if (LeakCanary.isInAnalyzerProcess(this)) {
      // This process is dedicated to LeakCanary for heap analysis.
      return
    }
    LazyThreeTen.init(this)
    DaggerApplicationComponent.builder()
        .application(this)
        .build()
        .inject(this)
    P.init(this, false)
    P.setSharedPreferences(sharedPreferences, rxPreferences)
    initVariant()

    remoteConfig.fetch(resources.getInteger(R.integer.remote_config_cache_duration).toLong())
        .addOnCompleteListener { task ->
          if (task.isSuccessful) {
            d { "Firebase fetch succeeded" }
            remoteConfig.activateFetched()
          } else {
            d { "Firebase fetch failed" }
          }
        }
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
        sharedPreferences.getBoolean(P.reports.KEY, false)
  }

  protected open fun initVariant() {
    // Override this in variants
  }

  override fun activityInjector(): DispatchingAndroidInjector<Activity> {
    return dispatchingActivityInjector
  }

  override fun onTrimMemory(level: Int) {
    super.onTrimMemory(level)
    Glide.with(this)
        .onTrimMemory(level)
    when (level) {
      TRIM_MEMORY_MODERATE,
      TRIM_MEMORY_RUNNING_LOW,
      TRIM_MEMORY_RUNNING_MODERATE,
      TRIM_MEMORY_RUNNING_CRITICAL -> d { "OnTrimMemory" }
    }
  }
}
