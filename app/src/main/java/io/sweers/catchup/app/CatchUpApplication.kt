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
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.jakewharton.threetenabp.AndroidThreeTen
import com.squareup.leakcanary.LeakCanary
import com.squareup.leakcanary.RefWatcher
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasActivityInjector
import io.sweers.catchup.P
import io.sweers.catchup.R
import io.sweers.catchup.data.LumberYard
import timber.log.Timber
import javax.inject.Inject

open class CatchUpApplication : Application(), HasActivityInjector {
  @Inject lateinit internal var dispatchingActivityInjector: DispatchingAndroidInjector<Activity>
  @Inject lateinit var sharedPreferences: SharedPreferences
  @Inject lateinit var lumberYard: LumberYard
  @Inject lateinit var remoteConfig: FirebaseRemoteConfig

  override fun onCreate() {
    super.onCreate()
    if (LeakCanary.isInAnalyzerProcess(this)) {
      // This process is dedicated to LeakCanary for heap analysis.
      return
    }
    component = DaggerApplicationComponent.builder()
        .application(this)
        .build()
    component.inject(this)
    AndroidThreeTen.init(this)
    P.init(this)
    P.setSharedPreferences(
        sharedPreferences)  // TODO Pass RxSharedPreferences instance to this when it's supported

    var nightMode = AppCompatDelegate.MODE_NIGHT_NO
    if (P.daynightAuto.get()) {
      nightMode = AppCompatDelegate.MODE_NIGHT_AUTO
    } else if (P.daynightNight.get()) {
      nightMode = AppCompatDelegate.MODE_NIGHT_YES
    }
    AppCompatDelegate.setDefaultNightMode(nightMode)
    initVariant()
    remoteConfig.fetch(resources.getInteger(R.integer.remote_config_cache_duration).toLong())
        .addOnCompleteListener { task ->
          if (task.isSuccessful) {
            Timber.d("Firebase fetch succeeded")
            remoteConfig.activateFetched()
          } else {
            Timber.d("Firebase fetch failed")
          }
        }
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
      TRIM_MEMORY_RUNNING_CRITICAL -> Timber.d("OnTrimMemory")
    }// TODO someday clear Store in-memory
  }

  companion object {

    protected lateinit var refWatcher: RefWatcher
    private lateinit var component: ApplicationComponent

    @JvmStatic fun component(): ApplicationComponent {
      return component
    }

    @JvmStatic fun refWatcher(): RefWatcher {
      return refWatcher
    }
  }
}
