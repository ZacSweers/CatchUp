package io.sweers.catchup.ui.activity;

import android.support.v7.app.ActionBar;

import dagger.Provides;
import io.sweers.catchup.injection.PerActivity;
import io.sweers.catchup.util.customtabs.CustomTabActivityHelper;

@dagger.Module
class ActivityModule {

  private MainActivity activity;

  public ActivityModule(MainActivity activity) {
    this.activity = activity;
  }

  @PerActivity
  @Provides
  ActionBar provideActionBar() {
    return activity.getSupportActionBar();
  }

  @PerActivity
  @Provides
  CustomTabActivityHelper provideCustomTabActivityHelper() {
    return new CustomTabActivityHelper(activity);
  }

}
