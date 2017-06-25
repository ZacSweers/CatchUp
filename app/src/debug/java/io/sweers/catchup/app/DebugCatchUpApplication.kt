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
import android.os.Bundle
import com.facebook.stetho.Stetho
import com.readystatesoftware.chuck.internal.ui.MainActivity
import com.squareup.leakcanary.LeakCanary
import timber.log.Timber

class DebugCatchUpApplication : CatchUpApplication() {
  override fun initVariant() {
    refWatcher = LeakCanary.refWatcher(this).build()
    registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
      override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle) {}

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
    Stetho.initializeWithDefaults(this)
  }
}
