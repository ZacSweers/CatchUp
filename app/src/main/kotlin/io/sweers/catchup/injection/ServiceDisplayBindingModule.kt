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

package io.sweers.catchup.injection

import dagger.Module
import dagger.android.ContributesAndroidInjector
import io.sweers.catchup.data.smmry.SmmryModule
import io.sweers.catchup.injection.scopes.PerFragment
import io.sweers.catchup.ui.fragments.PagerFragment
import io.sweers.catchup.ui.fragments.SmmryFragment
import io.sweers.catchup.ui.fragments.service.ServiceFragment

@Module
abstract class ServiceDisplayBindingModule {

  @PerFragment
  @ContributesAndroidInjector
  internal abstract fun serviceFragment(): ServiceFragment

  @PerFragment
  @ContributesAndroidInjector(modules = [SmmryModule::class])
  internal abstract fun smmryFragment(): SmmryFragment

  @PerFragment
  @ContributesAndroidInjector(modules = [PagerFragment.Module::class])
  internal abstract fun pagerFragment(): PagerFragment

}
