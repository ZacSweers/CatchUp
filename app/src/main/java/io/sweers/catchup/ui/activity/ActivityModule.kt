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

package io.sweers.catchup.ui.activity

import android.app.Activity
import com.f2prateek.rx.preferences2.Preference
import com.f2prateek.rx.preferences2.RxSharedPreferences
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.android.ActivityKey
import dagger.android.AndroidInjector
import dagger.multibindings.IntoMap
import io.sweers.catchup.P
import io.sweers.catchup.data.LinkManager
import io.sweers.catchup.injection.qualifiers.preferences.SmartLinking
import io.sweers.catchup.util.customtabs.CustomTabActivityHelper

@Module(includes = arrayOf(UiModule::class), subcomponents = arrayOf(ActivityComponent::class))
abstract class ActivityModule {

  @Binds
  @IntoMap
  @ActivityKey(MainActivity::class)
  internal abstract fun bindYourActivityInjectorFactory(
      builder: ActivityComponent.Builder): AndroidInjector.Factory<out Activity>

  @Module
  companion object {

    @Provides @JvmStatic internal fun provideCustomTabActivityHelper(): CustomTabActivityHelper {
      return CustomTabActivityHelper()
    }

    @Provides
    @SmartLinking
    @JvmStatic
    internal fun provideSmartLinkingPref(
        rxSharedPreferences: RxSharedPreferences): Preference<Boolean> {
      // TODO Use psync once it's fixed
      return rxSharedPreferences.getBoolean(P.SmartlinkingGlobal.KEY,
          P.SmartlinkingGlobal.defaultValue())
      //    return P.smartlinkingGlobal.rx();
    }

    @Provides @JvmStatic internal fun provideLinkManager(helper: CustomTabActivityHelper,
        @SmartLinking linkingPref: Preference<Boolean>): LinkManager {
      return LinkManager(helper, linkingPref)
    }
  }
}
