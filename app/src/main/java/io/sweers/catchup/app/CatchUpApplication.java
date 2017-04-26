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
import android.app.Application;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatDelegate;
import com.jakewharton.threetenabp.AndroidThreeTen;
import com.squareup.leakcanary.LeakCanary;
import com.squareup.leakcanary.RefWatcher;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.HasDispatchingActivityInjector;
import io.sweers.catchup.P;
import io.sweers.catchup.data.LumberYard;
import javax.inject.Inject;

public class CatchUpApplication extends Application implements HasDispatchingActivityInjector {

  protected static RefWatcher refWatcher;
  private static ApplicationComponent component;
  @Inject DispatchingAndroidInjector<Activity> dispatchingActivityInjector;
  @Inject protected SharedPreferences sharedPreferences;
  @Inject protected LumberYard lumberYard;

  public static ApplicationComponent component() {
    return component;
  }

  public static RefWatcher refWatcher() {
    return refWatcher;
  }

  @Override public void onCreate() {
    super.onCreate();
    //noinspection ConstantConditions
    if (LeakCanary.isInAnalyzerProcess(this)) {
      // This process is dedicated to LeakCanary for heap analysis.
      return;
    }
    component = DaggerApplicationComponent.builder()
        .application(this)
        .build();
    component.inject(this);
    AndroidThreeTen.init(this);
    P.init(this);
    P.setSharedPreferences(sharedPreferences);  // TODO Pass RxSharedPreferences instance to this when it's supported

    int nightMode = AppCompatDelegate.MODE_NIGHT_NO;
    if (P.daynightAuto.get()) {
      nightMode = AppCompatDelegate.MODE_NIGHT_AUTO;
    } else if (P.daynightNight.get()) {
      nightMode = AppCompatDelegate.MODE_NIGHT_YES;
    }
    AppCompatDelegate.setDefaultNightMode(nightMode);
    initVariant();
  }

  protected void initVariant() {
    // Override this in variants
  }

  @Override public DispatchingAndroidInjector<Activity> activityInjector() {
    return dispatchingActivityInjector;
  }
}
