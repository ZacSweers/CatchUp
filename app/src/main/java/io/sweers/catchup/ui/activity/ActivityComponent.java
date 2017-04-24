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

import dagger.Subcomponent;
import dagger.android.AndroidInjector;
import io.sweers.catchup.ui.controllers.DesignerNewsController;
import io.sweers.catchup.ui.controllers.DribbbleController;
import io.sweers.catchup.ui.controllers.GitHubController;
import io.sweers.catchup.ui.controllers.HackerNewsController;
import io.sweers.catchup.ui.controllers.MediumController;
import io.sweers.catchup.ui.controllers.PagerController;
import io.sweers.catchup.ui.controllers.ProductHuntController;
import io.sweers.catchup.ui.controllers.RedditController;
import io.sweers.catchup.ui.controllers.SlashdotController;

@Subcomponent(modules = {
    UiModule.class,
    PagerController.Module.class,
    HackerNewsController.Module.class,
    RedditController.Module.class,
    MediumController.Module.class,
    ProductHuntController.Module.class,
    SlashdotController.Module.class,
    DesignerNewsController.Module.class,
    DribbbleController.Module.class,
    GitHubController.Module.class,
})
public interface ActivityComponent extends AndroidInjector<MainActivity> {

  @Subcomponent.Builder
  abstract class Builder extends AndroidInjector.Builder<MainActivity> {

  }
}
