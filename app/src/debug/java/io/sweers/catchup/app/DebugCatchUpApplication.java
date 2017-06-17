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

package io.sweers.catchup.app;

import android.app.Activity;
import android.os.Bundle;
import com.facebook.stetho.Stetho;
import com.readystatesoftware.chuck.internal.ui.MainActivity;
import com.squareup.leakcanary.LeakCanary;
import timber.log.Timber;

public final class DebugCatchUpApplication extends CatchUpApplication {
  @Override protected void initVariant() {
    refWatcher = LeakCanary.refWatcher(this)
        .build();
    registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
      @Override public void onActivityCreated(Activity activity, Bundle savedInstanceState) { }

      @Override public void onActivityStarted(Activity activity) { }

      @Override public void onActivityResumed(Activity activity) { }

      @Override public void onActivityPaused(Activity activity) { }

      @Override public void onActivityStopped(Activity activity) { }

      @Override public void onActivitySaveInstanceState(Activity activity, Bundle outState) { }

      @Override public void onActivityDestroyed(Activity activity) {
        if (activity instanceof MainActivity) {
          // Ignore Chuck
          return;
        }
        refWatcher.watch(activity);
      }
    });
    Timber.plant(new Timber.DebugTree());
    Timber.plant(getLumberYard().tree());
    Stetho.initializeWithDefaults(this);
  }
}
