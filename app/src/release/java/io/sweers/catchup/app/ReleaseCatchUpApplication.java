package io.sweers.catchup.app;

import com.bugsnag.android.Bugsnag;
import com.squareup.leakcanary.RefWatcher;
import io.sweers.catchup.BuildConfig;
import timber.log.Timber;

public final class ReleaseCatchUpApplication extends CatchUpApplication {
  @Override protected void initVariant() {
    refWatcher = RefWatcher.DISABLED;
    Bugsnag.init(this, BuildConfig.BUGSNAG_KEY);

    final BugsnagTree tree = new BugsnagTree();
    Bugsnag.getClient()
        .beforeNotify(error -> {
          tree.update(error);
          return true;
        });

    Timber.plant(tree);
  }
}
