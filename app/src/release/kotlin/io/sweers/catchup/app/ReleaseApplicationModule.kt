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
package io.sweers.catchup.app

import android.app.Application
import com.bugsnag.android.Bugsnag
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import io.sweers.catchup.BuildConfig
import io.sweers.catchup.app.ApplicationModule.Initializers
import io.sweers.catchup.base.ui.CatchUpObjectWatcher
import timber.log.Timber
import javax.inject.Qualifier
import kotlin.annotation.AnnotationRetention.BINARY

@Module
object ReleaseApplicationModule {

  @JvmStatic
  @Provides
  fun provideObjectWatcher(): CatchUpObjectWatcher = CatchUpObjectWatcher.None

  @Qualifier
  @Retention(BINARY)
  private annotation class BugsnagKey

  @BugsnagKey
  @JvmStatic
  @Provides
  fun provideBugsnagKey(): String = BuildConfig.BUGSNAG_KEY

  @Initializers
  @JvmStatic
  @IntoSet
  @Provides
  fun bugsnagInit(application: Application, @BugsnagKey key: String): () -> Unit = {
    Bugsnag.init(application, key)
  }

  @JvmStatic
  @IntoSet
  @Provides
  fun provideBugsnagTree(): Timber.Tree = BugsnagTree().also {
    Bugsnag.getClient()
        .beforeNotify { error ->
          it.update(error)
          true
        }
  }
}
