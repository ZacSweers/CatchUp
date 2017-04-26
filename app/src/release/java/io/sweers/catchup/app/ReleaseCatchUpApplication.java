package io.sweers.catchup.app;

import com.squareup.leakcanary.RefWatcher;

public final class ReleaseCatchUpApplication extends CatchUpApplication {
  @Override protected void initVariant() {
    refWatcher = RefWatcher.DISABLED;
//    Bugsnag.init(this, "");

//    final BugsnagTree tree = new BugsnagTree();
//    Bugsnag.getClient().beforeNotify(error -> {
//      tree.update(error);
//      return true;
//    });
//
//    Timber.plant(tree);
  }
}
