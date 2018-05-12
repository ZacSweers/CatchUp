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

import com.bluelinelabs.conductor.Controller
import dagger.Binds
import dagger.Module
import dagger.android.AndroidInjector
import dagger.multibindings.IntoMap
import io.sweers.catchup.ui.controllers.PagerController
import io.sweers.catchup.ui.controllers.SmmryController
import io.sweers.catchup.ui.controllers.service.ServiceController

@Module(
    subcomponents = [
    ServiceController.Component::class,
    PagerController.Component::class,
    SmmryController.Component::class
    ]
)
abstract class ControllerBindingModule {
  @Binds
  @IntoMap
  @ControllerKey(ServiceController::class)
  internal abstract fun bindNewServiceControllerInjectorFactory(
      builder: ServiceController.Component.Builder): AndroidInjector.Factory<out Controller>

  @Binds
  @IntoMap
  @ControllerKey(SmmryController::class)
  internal abstract fun bindSmmryControllerInjectorFactory(
      builder: SmmryController.Component.Builder): AndroidInjector.Factory<out Controller>

  @Binds
  @IntoMap
  @ControllerKey(PagerController::class)
  internal abstract fun bindPagerControllerInjectorFactory(
      builder: PagerController.Component.Builder): AndroidInjector.Factory<out Controller>

}
