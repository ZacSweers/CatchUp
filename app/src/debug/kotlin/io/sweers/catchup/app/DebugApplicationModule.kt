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

import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import android.os.strictmode.DiskReadViolation
import android.os.strictmode.UntaggedSocketViolation
import android.util.Log
import com.facebook.flipper.android.AndroidFlipperClient
import com.facebook.flipper.android.utils.FlipperUtils
import com.facebook.flipper.core.FlipperPlugin
import com.facebook.flipper.plugins.crashreporter.CrashReporterPlugin
import com.facebook.soloader.SoLoader
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import io.sweers.catchup.app.ApplicationModule.AsyncInitializers
import io.sweers.catchup.app.ApplicationModule.Initializers
import io.sweers.catchup.base.ui.CatchUpObjectWatcher
import io.sweers.catchup.data.LumberYard
import io.sweers.catchup.util.sdk
import leakcanary.AppWatcher
import leakcanary.LeakCanary
import shark.AndroidReferenceMatchers
import timber.log.Timber
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import kotlin.annotation.AnnotationRetention.BINARY

@Module
object DebugApplicationModule {

  @Qualifier
  @Retention(BINARY)
  private annotation class LeakCanaryEnabled

  /**
   * Disabled on API 28 because there's a pretty vicious memory leak that constantly triggers
   * https://github.com/square/leakcanary/issues/1081
   */
  @JvmStatic // https://github.com/google/dagger/issues/1648
  @LeakCanaryEnabled
  @Provides
  fun provideLeakCanaryEnabled(): Boolean = Build.VERSION.SDK_INT != 28

  @Provides
  fun provideObjectWatcher(@LeakCanaryEnabled leakCanaryEnabled: Boolean): CatchUpObjectWatcher {
    return if (leakCanaryEnabled) {
      object : CatchUpObjectWatcher {
        override fun watch(watchedReference: Any) {
          AppWatcher.objectWatcher.watch(watchedReference)
        }
      }
    } else {
      CatchUpObjectWatcher.None
    }
  }

  @Provides
  fun provideLeakCanaryConfig(@LeakCanaryEnabled leakCanaryEnabled: Boolean): LeakCanary.Config {
    return if (leakCanaryEnabled) {
      LeakCanary.config.copy(referenceMatchers = AndroidReferenceMatchers.appDefaults)
    } else {
      LeakCanary.config
    }
  }

  @Initializers
  @IntoSet
  @Provides
  fun leakCanaryInit(
    application: Application,
    objectWatcher: CatchUpObjectWatcher,
    leakCanaryConfig: LeakCanary.Config
  ): () -> Unit = {
    AppWatcher.config.copy(
        watchDurationMillis = TimeUnit.SECONDS.toMillis(10)
    )
    LeakCanary.config = leakCanaryConfig

    application.registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
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
        objectWatcher.watch(activity)
      }
    })
  }

  @Qualifier
  @Retention(BINARY)
  private annotation class StrictModeExecutor

  @StrictModeExecutor
  @Provides
  fun strictModeExecutor(): ExecutorService = Executors.newSingleThreadExecutor()

  @Initializers
  @IntoSet
  @Provides
  fun strictModeInit(@StrictModeExecutor penaltyListenerExecutor: dagger.Lazy<ExecutorService>): () -> Unit = {
    StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder()
        .detectAll()
        .apply {
          sdk(28) {
            penaltyListener(penaltyListenerExecutor.get(), StrictMode.OnThreadViolationListener {
              Timber.w(it)
            }) ?: run {
              penaltyLog()
            }
          }
        }
        .build())
    StrictMode.setVmPolicy(VmPolicy.Builder()
        .detectAll()
        .penaltyLog()
        .apply {
          sdk(28) {
            penaltyListener(penaltyListenerExecutor.get(), StrictMode.OnVmViolationListener {
              when (it) {
                is UntaggedSocketViolation -> {
                  // Firebase and OkHttp don't tag sockets
                  return@OnVmViolationListener
                }
                is DiskReadViolation -> {
                  if (it.stackTrace.any { it.methodName == "onCreatePreferences" }) {
                    // PreferenceFragment hits preferences directly
                    return@OnVmViolationListener
                  }
                }
              }
              // Note: Chuck causes a closeable leak. Possible https://github.com/square/okhttp/issues/3174
              Timber.w(it)
            })
          } ?: run {
            penaltyLog()
          }
        }
        .build())
  }

  @Qualifier
  @Retention(BINARY)
  private annotation class FlipperEnabled

  @JvmStatic // https://github.com/google/dagger/issues/1648
  @FlipperEnabled
  @Provides
  fun provideFlipperEnabled(application: Application): Boolean {
    return if (Build.VERSION.SDK_INT == 28) {
      // Flipper native crashes on this
      false
    } else {
      FlipperUtils.shouldEnableFlipper(application)
    }
  }

  @AsyncInitializers
  @IntoSet
  @Provides
  fun flipperInit(
    @FlipperEnabled enabled: Boolean,
    application: Application,
    flipperPlugins: Set<@JvmSuppressWildcards FlipperPlugin>
  ): () -> Unit = {
    if (enabled) {
      SoLoader.init(application, SoLoader.SOLOADER_ALLOW_ASYNC_INIT)
      AndroidFlipperClient.getInstance(application).apply {
        flipperPlugins.forEach(::addPlugin)
        start()
      }
    }
  }

  @IntoSet
  @Provides
  fun provideDebugTree(): Timber.Tree = Timber.DebugTree()

  @IntoSet
  @Provides
  fun provideLumberYardTree(lumberYard: LumberYard): Timber.Tree = lumberYard.tree()

  @IntoSet
  @Provides
  fun provideCrashOnErrorTree(flipperCrashReporter: CrashReporterPlugin): Timber.Tree {
    return object : Timber.Tree() {
      override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (priority == Log.ERROR) {
          val exception = RuntimeException("Timber e! Please fix:\nTag=$tag\nMessage=$message", t)
          // Show this in the Flipper heads up notification
          flipperCrashReporter.sendExceptionMessage(
              Thread.currentThread(),
              exception
          )
        }
      }
    }
  }
}
