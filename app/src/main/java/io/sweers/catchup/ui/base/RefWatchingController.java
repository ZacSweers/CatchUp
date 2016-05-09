package io.sweers.catchup.ui.base;

import android.os.Bundle;

import io.sweers.catchup.app.CatchUpApplication;

public abstract class RefWatchingController extends ButterKnifeController {

  protected RefWatchingController() {
  }

  protected RefWatchingController(Bundle args) {
    super(args);
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    CatchUpApplication.refWatcher().watch(this);
  }

}
