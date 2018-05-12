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
import dagger.Provides
import io.sweers.catchup.injection.scopes.PerActivity
import io.sweers.catchup.ui.DebugViewContainer
import io.sweers.catchup.ui.ViewContainer
import io.sweers.catchup.ui.bugreport.BugReportModule

@Module(includes = [BugReportModule::class])
object UiModule {

  @Provides
  @JvmStatic
  @PerActivity
  internal fun provideViewContainer(viewContainer: DebugViewContainer): ViewContainer =
      viewContainer
}
