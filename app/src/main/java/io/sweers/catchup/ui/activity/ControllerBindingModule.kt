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

import dagger.Module
import dagger.android.ContributesAndroidInjector
import io.sweers.catchup.injection.scopes.PerController
import io.sweers.catchup.ui.controllers.DesignerNewsController
import io.sweers.catchup.ui.controllers.DribbbleController
import io.sweers.catchup.ui.controllers.GitHubController
import io.sweers.catchup.ui.controllers.HackerNewsController
import io.sweers.catchup.ui.controllers.MediumController
import io.sweers.catchup.ui.controllers.ProductHuntController
import io.sweers.catchup.ui.controllers.RedditController
import io.sweers.catchup.ui.controllers.SlashdotController

@Module
abstract class ControllerBindingModule {

  @PerController
  @ContributesAndroidInjector(modules = arrayOf(HackerNewsController.Module::class))
  abstract fun hackerNewsController(): HackerNewsController

  @PerController
  @ContributesAndroidInjector(modules = arrayOf(RedditController.Module::class))
  abstract fun redditController(): RedditController

  @PerController
  @ContributesAndroidInjector(modules = arrayOf(MediumController.Module::class))
  abstract fun mediumController(): MediumController

  @PerController
  @ContributesAndroidInjector(modules = arrayOf(ProductHuntController.Module::class))
  abstract fun productHuntController(): ProductHuntController

  @PerController
  @ContributesAndroidInjector(modules = arrayOf(SlashdotController.Module::class))
  abstract fun slashdotController(): SlashdotController

  @PerController
  @ContributesAndroidInjector(modules = arrayOf(DesignerNewsController.Module::class))
  abstract fun designerNewsController(): DesignerNewsController

  @PerController
  @ContributesAndroidInjector(modules = arrayOf(DribbbleController.Module::class))
  abstract fun dribbbleController(): DribbbleController

  @PerController
  @ContributesAndroidInjector(modules = arrayOf(GitHubController.Module::class))
  abstract fun gitHubController(): GitHubController
}
