package io.sweers.catchup.ui.base;

import android.os.Bundle;
import com.bluelinelabs.conductor.Controller;
import io.sweers.catchup.app.CatchUpApplication;

public abstract class RefWatchingController extends Controller {

  protected RefWatchingController() {
    super();
  }

  protected RefWatchingController(Bundle args) {
    super(args);
  }

  @Override public void onDestroy() {
    super.onDestroy();
    CatchUpApplication.refWatcher()
        .watch(this);
  }
}
