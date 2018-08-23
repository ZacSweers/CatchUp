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

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import android.util.Log
import androidx.lifecycle.ReportFragment
import com.facebook.soloader.SoLoader
import com.facebook.sonar.android.AndroidSonarClient
import com.facebook.sonar.android.utils.SonarUtils
import com.facebook.sonar.core.SonarPlugin
import com.facebook.stetho.Stetho
import com.facebook.stetho.timber.StethoTree
import com.readystatesoftware.chuck.internal.ui.MainActivity
import com.squareup.leakcanary.AndroidExcludedRefs
import com.squareup.leakcanary.LeakCanary
import io.reactivex.Completable
import io.reactivex.schedulers.Schedulers
import io.sweers.catchup.BuildConfig
import timber.log.Timber
import timber.log.Timber.Tree
import javax.inject.Inject

class DebugCatchUpApplication : CatchUpApplication() {

  @Inject
  lateinit var flipperPlugins: Set<@JvmSuppressWildcards SonarPlugin>

  override fun onPreInject() {
    SoLoader.init(this, false)
  }

  override fun inject() {
    DaggerApplicationComponent.builder()
        .application(this)
        .build()
        .inject(this)
  }

  override fun initVariant() {
    StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder()
        .detectAll()
        .penaltyLog()
        .build())
    StrictMode.setVmPolicy(VmPolicy.Builder()
        .detectAll()  // Note: Chuck causes a closeable leak. Possible https://github.com/square/okhttp/issues/3174
        .penaltyLog()
        .build())
    refWatcher = LeakCanary.refWatcher(this)
        .excludedRefs(AndroidExcludedRefs.createAppDefaults()
            .instanceField("android.view.ViewGroup\$ViewLocationHolder", "mRoot") // https://github.com/square/leakcanary/issues/1081
            .build())
        .buildAndInstall()
    registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
      override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

      override fun onActivityStarted(activity: Activity) {}

      override fun onActivityResumed(activity: Activity) {}

      override fun onActivityPaused(activity: Activity) {}

      override fun onActivityStopped(activity: Activity) {}

      override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

      override fun onActivityDestroyed(activity: Activity) {
        if (activity is MainActivity) {
          // Ignore Chuck
          return
        }
        refWatcher.watch(activity)
      }
    })
    Timber.plant(Timber.DebugTree())
    Timber.plant(lumberYard.tree())
    Timber.plant(StethoTree())

    if (SonarUtils.shouldEnableSonar(this)) {
      Completable
          .fromAction {
            AndroidSonarClient.getInstance(this).apply {
              flipperPlugins.forEach(::addPlugin)
              start()
            }
          }
          .subscribeOn(Schedulers.io())
          .subscribe()
    }

    if (BuildConfig.CRASH_ON_TIMBER_ERROR) {
      Timber.plant(object : Tree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
          if (priority == Log.ERROR) {
            throw RuntimeException("Timber e! Please fix:\nTag=$tag\nMessage=$message", t)
          }
        }
      })
    }
    Completable
        .fromAction {
          Stetho.initializeWithDefaults(this)
        }
        .subscribeOn(Schedulers.io())
        .subscribe()
  }
}
