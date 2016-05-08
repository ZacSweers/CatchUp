package io.sweers.catchup.ui.activity;

import dagger.Provides;
import io.sweers.catchup.injection.PerActivity;
import io.sweers.catchup.ui.base.ActionBarProvider;
import io.sweers.catchup.util.customtabs.CustomTabActivityHelper;

@dagger.Module
class ActivityModule {

  private MainActivity activity;

  public ActivityModule(MainActivity activity) {
    this.activity = activity;
  }

  @PerActivity
  @Provides
  MainActivity provideActivity() {
    return activity;
  }

  @PerActivity
  @Provides
  ActionBarProvider provideActionBar() {
    return activity;
  }

  @PerActivity
  @Provides
  CustomTabActivityHelper provideCustomTabActivityHelper() {
    return new CustomTabActivityHelper(activity);
  }

}
