package io.sweers.catchup.ui.activity;

import com.f2prateek.rx.preferences.Preference;
import com.f2prateek.rx.preferences.RxSharedPreferences;

import dagger.Provides;
import io.sweers.catchup.P;
import io.sweers.catchup.injection.qualifiers.preferences.SmartLinking;
import io.sweers.catchup.injection.scopes.PerActivity;
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

  @PerActivity
  @Provides
  @SmartLinking
  Preference<Boolean> provideSmartLinkingPref(RxSharedPreferences rxSharedPreferences) {
    // TODO Use psync once it's fixed
    return rxSharedPreferences.getBoolean(P.smartlinkingGlobal.key, P.smartlinkingGlobal.defaultValue());
//    return P.smartlinkingGlobal.rx();
  }

}
