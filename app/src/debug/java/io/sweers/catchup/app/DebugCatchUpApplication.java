package io.sweers.catchup.app;

import com.facebook.stetho.Stetho;

import timber.log.Timber;

public final class DebugCatchUpApplication extends CatchUpApplication {
  @Override protected void initVariant() {
    Timber.plant(new Timber.DebugTree());
    Stetho.initializeWithDefaults(this);
  }
}
