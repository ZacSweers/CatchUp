package io.sweers.catchup.app;

import android.app.Activity;
import android.app.Application;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatDelegate;
import com.jakewharton.threetenabp.AndroidThreeTen;
import com.squareup.leakcanary.LeakCanary;
import com.squareup.leakcanary.RefWatcher;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.HasDispatchingActivityInjector;
import io.sweers.catchup.P;
import io.sweers.catchup.data.LumberYard;
import io.sweers.catchup.injection.Modules;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;

public class CatchUpApplication extends Application implements HasDispatchingActivityInjector {

  private static RefWatcher refWatcher;
  private static ApplicationComponent component;
  @Inject DispatchingAndroidInjector<Activity> dispatchingActivityInjector;
  @Inject protected SharedPreferences sharedPreferences;
  @Inject protected LumberYard lumberYard;

  @NonNull public static ApplicationComponent component() {
    return component;
  }

  @NonNull public static RefWatcher refWatcher() {
    return refWatcher;
  }

  @Override public void onCreate() {
    super.onCreate();
    if (LeakCanary.isInAnalyzerProcess(this)) {
      // This process is dedicated to LeakCanary for heap analysis.
      return;
    }
    refWatcher = LeakCanary.refWatcher(this)
        .watchDelay(10, TimeUnit.SECONDS)
        .buildAndInstall();
    component = DaggerApplicationComponent.builder()
        .application(this)
        .dataModule(Modules.dataModule())
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
