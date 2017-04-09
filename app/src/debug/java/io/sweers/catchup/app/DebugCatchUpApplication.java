package io.sweers.catchup.app;

import com.facebook.stetho.Stetho;
import com.squareup.leakcanary.LeakCanary;
import java.util.concurrent.TimeUnit;
import timber.log.Timber;

public final class DebugCatchUpApplication extends CatchUpApplication {
  @Override protected void initVariant() {
    refWatcher = LeakCanary.refWatcher(this)
        .watchDelay(10, TimeUnit.SECONDS)
        .buildAndInstall();
    Timber.plant(new Timber.DebugTree());
    Timber.plant(lumberYard.tree());
    Stetho.initializeWithDefaults(this);
  }
}
