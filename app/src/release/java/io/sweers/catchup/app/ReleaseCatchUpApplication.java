package io.sweers.catchup.app;

import com.bugsnag.android.Bugsnag;

import timber.log.Timber;

public final class ReleaseCatchUpApplication extends CatchUpApplication {
  @Override protected void initVariant() {
    Bugsnag.init(this, "");

    final BugsnagTree tree = new BugsnagTree();
    Bugsnag.getClient().beforeNotify(error -> {
      tree.update(error);
      return true;
    });

    Timber.plant(tree);
  }
}
