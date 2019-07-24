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
import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import android.util.Log
import com.facebook.flipper.android.AndroidFlipperClient
import com.facebook.flipper.android.utils.FlipperUtils
import com.facebook.flipper.core.FlipperPlugin
import com.facebook.soloader.SoLoader
import com.facebook.stetho.Stetho
import com.facebook.stetho.timber.StethoTree
import io.sweers.catchup.BuildConfig
import io.sweers.catchup.util.sdk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import leakcanary.AndroidKnownReference
import leakcanary.LeakCanary
import leakcanary.LeakSentry
import timber.log.Timber
import timber.log.Timber.Tree
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.LazyThreadSafetyMode.NONE

class DebugCatchUpApplication : CatchUpApplication() {

  @Inject
  lateinit var flipperPlugins: Set<@JvmSuppressWildcards FlipperPlugin>

  override fun onPreInject() {
    SoLoader.init(this, false)
  }

  override fun inject() {
    DaggerApplicationComponent.factory()
        .create(this)
        .inject(this)
  }

  @SuppressLint("InlinedApi") // False positive
  override fun initVariant() {
    val penaltyListenerExecutor by lazy(NONE) {
      Executors.newSingleThreadExecutor()
    }
    StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder()
        .detectAll()
        .penaltyLog()
        .apply {
          sdk(28) {
            penaltyListener(penaltyListenerExecutor, StrictMode.OnThreadViolationListener {
              Timber.w(it)
            })
          }
        }
        .build())
    StrictMode.setVmPolicy(VmPolicy.Builder()
        .detectAll()
        .penaltyLog()
        .apply {
          sdk(28) {
            penaltyListener(penaltyListenerExecutor, StrictMode.OnVmViolationListener {
              // Note: Chuck causes a closeable leak. Possible https://github.com/square/okhttp/issues/3174
              Timber.w(it)
            })
          }
        }
        .build())
    LeakSentry.config.copy(
        watchDurationMillis = TimeUnit.SECONDS.toMillis(10)
    )
    LeakCanary.config = if (Build.VERSION.SDK_INT != 28) {
      refWatcher = object : CatchUpRefWatcher {
        override fun watch(watchedReference: Any) {
          LeakSentry.refWatcher.watch(watchedReference)
        }
      }
      LeakCanary.config.copy(knownReferences = AndroidKnownReference.appDefaults)
    } else {
      // Disabled on API 28 because there's a pretty vicious memory leak that constantly triggers
      // https://github.com/square/leakcanary/issues/1081
      refWatcher = CatchUpRefWatcher.None
      LeakCanary.config.copy()
    }
    registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
      override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

      override fun onActivityStarted(activity: Activity) {}

      override fun onActivityResumed(activity: Activity) {}

      override fun onActivityPaused(activity: Activity) {}

      override fun onActivityStopped(activity: Activity) {}

      override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

      override fun onActivityDestroyed(activity: Activity) {
//        if (activity is MainActivity) {
//          // Ignore Chuck
//          return
//        }
        refWatcher.watch(activity)
      }
    })
    Timber.plant(Timber.DebugTree())
    Timber.plant(lumberYard.tree())
    Timber.plant(StethoTree())

    if (BuildConfig.CRASH_ON_TIMBER_ERROR) {
      Timber.plant(object : Tree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
          if (priority == Log.ERROR) {
            throw RuntimeException("Timber e! Please fix:\nTag=$tag\nMessage=$message", t)
          }
        }
      })
    }

    GlobalScope.launch(Dispatchers.IO) {
      if (FlipperUtils.shouldEnableFlipper(this@DebugCatchUpApplication)) {
        AndroidFlipperClient.getInstance(this@DebugCatchUpApplication).apply {
          flipperPlugins.forEach(::addPlugin)
          start()
        }
      }
      Stetho.initializeWithDefaults(this@DebugCatchUpApplication)
    }
  }
}
