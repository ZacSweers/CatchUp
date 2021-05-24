/*
 * Copyright (C) 2019. Zac Sweers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.sweers.catchup.injection

import android.app.Activity
import androidx.activity.ComponentActivity
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.scopes.ActivityScoped
import io.sweers.catchup.serviceregistry.CatchUpServiceMetaRegistry
import io.sweers.catchup.serviceregistry.CatchUpServiceRegistry
import io.sweers.catchup.ui.activity.MainActivity

@InstallIn(ActivityComponent::class)
@Module(includes = [CatchUpServiceRegistry::class, CatchUpServiceMetaRegistry::class])
object ComponentActivityModule {
  @ActivityScoped
  @Provides
  fun provideComponentActivity(activity: Activity): ComponentActivity {
    return activity as ComponentActivity
  }
}

// This weirds me out
@InstallIn(ActivityComponent::class)
@Module
object MainActivityModule {
  @ActivityScoped
  @Provides
  fun provideComponentActivity(activity: Activity): MainActivity {
    return activity as MainActivity
  }
}
