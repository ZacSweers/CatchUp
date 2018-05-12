/*
 * Copyright (c) 2018 Zac Sweers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.sweers.catchup.ui.activity

import dagger.Module
import dagger.android.ContributesAndroidInjector
import io.sweers.catchup.injection.ControllerBindingModule
import io.sweers.catchup.injection.scopes.PerActivity
import io.sweers.catchup.ui.about.AboutActivity
import io.sweers.catchup.ui.about.AboutControllerBindingModule
import io.sweers.catchup.ui.activity.ServiceSettingsActivity.ServiceSettingsFrag.ServiceSettingsModule
import io.sweers.catchup.ui.activity.SettingsActivity.SettingsFrag.SettingsFragmentBindingModule

@Module
abstract class ActivityBindingModule {

  @PerActivity
  @ContributesAndroidInjector(modules = arrayOf(
      UiModule::class,
      MainActivity.ServiceIntegrationModule::class,
      ControllerBindingModule::class
  ))
  internal abstract fun mainActivity(): MainActivity

  @PerActivity
  @ContributesAndroidInjector(modules = arrayOf(
      UiModule::class,
      SettingsActivity.SettingsActivityModule::class,
      SettingsFragmentBindingModule::class
  ))
  internal abstract fun settingsActivity(): SettingsActivity

  @PerActivity
  @ContributesAndroidInjector(modules = arrayOf(
      UiModule::class,
      ServiceSettingsActivity.ServiceSettingsActivityModule::class,
      ServiceSettingsModule::class
  ))
  internal abstract fun serviceSettingsActivity(): ServiceSettingsActivity

  @PerActivity
  @ContributesAndroidInjector(modules = arrayOf(
      UiModule::class,
      AboutActivity.Module::class,
      AboutControllerBindingModule::class
  ))
  internal abstract fun aboutActivity(): AboutActivity

  @PerActivity
  @ContributesAndroidInjector(
      modules = [
        UiModule::class,
        OrderServicesActivity.Module::class,
        OrderServicesBindingModule::class
      ]
  )
  internal abstract fun orderServicesActivity(): OrderServicesActivity
}

// TODO Why can't I do this?
//interface BaseActivityModule<in T : Activity> {
//  @Binds
//  @PerActivity
//  fun provideActivity(activity: T): Activity
//}
