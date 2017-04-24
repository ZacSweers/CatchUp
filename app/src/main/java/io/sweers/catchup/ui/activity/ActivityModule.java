/*
 * Copyright (c) 2017 Zac Sweers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.sweers.catchup.ui.activity;

import android.app.Activity;
import com.f2prateek.rx.preferences.Preference;
import com.f2prateek.rx.preferences.RxSharedPreferences;
import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.android.ActivityKey;
import dagger.android.AndroidInjector;
import dagger.multibindings.IntoMap;
import io.sweers.catchup.P;
import io.sweers.catchup.data.LinkManager;
import io.sweers.catchup.injection.qualifiers.preferences.SmartLinking;
import io.sweers.catchup.util.customtabs.CustomTabActivityHelper;

@Module(includes = UiModule.class,
        subcomponents = ActivityComponent.class)
public abstract class ActivityModule {

  @Provides static CustomTabActivityHelper provideCustomTabActivityHelper() {
    return new CustomTabActivityHelper();
  }

  @Provides @SmartLinking
  static Preference<Boolean> provideSmartLinkingPref(RxSharedPreferences rxSharedPreferences) {
    // TODO Use psync once it's fixed
    return rxSharedPreferences.getBoolean(P.smartlinkingGlobal.key,
        P.smartlinkingGlobal.defaultValue());
    //    return P.smartlinkingGlobal.rx();
  }

  @Provides static LinkManager provideLinkManager(CustomTabActivityHelper helper,
      @SmartLinking Preference<Boolean> linkingPref) {
    return new LinkManager(helper, linkingPref);
  }

  @Binds @IntoMap @ActivityKey(MainActivity.class)
  abstract AndroidInjector.Factory<? extends Activity> bindYourActivityInjectorFactory(
      ActivityComponent.Builder builder);
}
