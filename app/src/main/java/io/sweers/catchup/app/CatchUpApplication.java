package io.sweers.catchup.app;

import android.app.Application;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatDelegate;

import com.facebook.stetho.Stetho;
import com.jakewharton.threetenabp.AndroidThreeTen;
import com.squareup.leakcanary.LeakCanary;
import com.squareup.leakcanary.RefWatcher;

import io.sweers.catchup.data.DataModule;
import timber.log.Timber;

public class CatchUpApplication extends Application {

  private static RefWatcher refWatcher;
  private static ApplicationComponent component;

  static {
    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO);
  }

  @Override
  public void onCreate() {
    super.onCreate();
    Timber.plant(new Timber.DebugTree());
    AndroidThreeTen.init(this);
    refWatcher = LeakCanary.install(this);
    component = DaggerApplicationComponent.builder()
        .applicationModule(new ApplicationModule(this))
        .dataModule(new DataModule())
        .build();
    component.inject(this);
    Stetho.initializeWithDefaults(this);
  }

  @NonNull
  public static ApplicationComponent component() {
    return component;
  }

  @NonNull
  public static RefWatcher refWatcher() {
    return refWatcher;
  }

}
