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

package io.sweers.catchup.app

import android.app.Application
import dagger.BindsInstance
import dagger.Component
import dagger.android.AndroidInjectionModule
import dagger.android.support.AndroidSupportInjectionModule
import io.sweers.catchup.data.DataModule
import io.sweers.catchup.ui.activity.ActivityBindingModule
import javax.inject.Singleton

@Singleton
@Component(modules = [
  ActivityBindingModule::class,
  AndroidSupportInjectionModule::class,
  AndroidInjectionModule::class,
  ApplicationModule::class,
  DataModule::class
])
interface ApplicationComponent {

  fun inject(application: ReleaseCatchUpApplication)

  @Component.Builder
  interface Builder {
    fun build(): ApplicationComponent

    @BindsInstance
    fun application(application: Application): Builder
  }
}
