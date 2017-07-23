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

package io.sweers.catchup.injection

import com.bluelinelabs.conductor.Controller
import dagger.Binds
import dagger.Module
import dagger.android.AndroidInjector
import dagger.multibindings.IntoMap
import io.sweers.catchup.ui.controllers.DesignerNewsController
import io.sweers.catchup.ui.controllers.DribbbleController
import io.sweers.catchup.ui.controllers.GitHubController
import io.sweers.catchup.ui.controllers.HackerNewsController
import io.sweers.catchup.ui.controllers.MediumController
import io.sweers.catchup.ui.controllers.ProductHuntController
import io.sweers.catchup.ui.controllers.RedditController
import io.sweers.catchup.ui.controllers.SlashdotController
import io.sweers.catchup.ui.controllers.SmmryController

@Module(
    subcomponents = arrayOf(
        SmmryController.Component::class,
        HackerNewsController.Component::class,
        RedditController.Component::class,
        MediumController.Component::class,
        ProductHuntController.Component::class,
        SlashdotController.Component::class,
        DribbbleController.Component::class,
        DesignerNewsController.Component::class,
        GitHubController.Component::class
    )
)
abstract class ControllerBindingModule {

  @Binds
  @IntoMap
  @ControllerKey(SmmryController::class)
  internal abstract fun bindSmmryControllerInjectorFactory(
      builder: SmmryController.Component.Builder): AndroidInjector.Factory<out Controller>

  @Binds
  @IntoMap
  @ControllerKey(HackerNewsController::class)
  internal abstract fun bindHackerNewsControllerInjectorFactory(
      builder: HackerNewsController.Component.Builder): AndroidInjector.Factory<out Controller>

  @Binds
  @IntoMap
  @ControllerKey(RedditController::class)
  internal abstract fun bindRedditControllerInjectorFactory(
      builder: RedditController.Component.Builder): AndroidInjector.Factory<out Controller>

  @Binds
  @IntoMap
  @ControllerKey(MediumController::class)
  internal abstract fun bindMediumControllerInjectorFactory(
      builder: MediumController.Component.Builder): AndroidInjector.Factory<out Controller>

  @Binds
  @IntoMap
  @ControllerKey(ProductHuntController::class)
  internal abstract fun bindProductHuntControllerInjectorFactory(
      builder: ProductHuntController.Component.Builder): AndroidInjector.Factory<out Controller>

  @Binds
  @IntoMap
  @ControllerKey(SlashdotController::class)
  internal abstract fun bindSlashdotControllerInjectorFactory(
      builder: SlashdotController.Component.Builder): AndroidInjector.Factory<out Controller>

  @Binds
  @IntoMap
  @ControllerKey(DribbbleController::class)
  internal abstract fun bindDribbbleControllerInjectorFactory(
      builder: DribbbleController.Component.Builder): AndroidInjector.Factory<out Controller>

  @Binds
  @IntoMap
  @ControllerKey(DesignerNewsController::class)
  internal abstract fun bindDesignerNewsControllerInjectorFactory(
      builder: DesignerNewsController.Component.Builder): AndroidInjector.Factory<out Controller>

  @Binds
  @IntoMap
  @ControllerKey(GitHubController::class)
  internal abstract fun bindGitHubControllerInjectorFactory(
      builder: GitHubController.Component.Builder): AndroidInjector.Factory<out Controller>

}
