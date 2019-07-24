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
import dagger.Binds
import dagger.Module
import io.sweers.catchup.injection.scopes.PerActivity

/**
 * TODO Why can't we not annotate this as Module
 */
@Module
interface ActivityModule<T : ComponentActivity> {
  @Binds
  @PerActivity
  fun provideComponentActivity(componentActivity: T): ComponentActivity

  @Binds
  @PerActivity
  fun provideActivity(componentActivity: T): Activity
}
