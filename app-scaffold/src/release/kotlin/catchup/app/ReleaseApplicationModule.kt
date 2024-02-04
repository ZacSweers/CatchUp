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
package catchup.app

import android.app.Application
import catchup.app.ApplicationModule.Initializers
import catchup.app.data.LumberYard
import catchup.base.ui.CatchUpObjectWatcher
import catchup.di.AppScope
import catchup.di.SingleIn
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Client
import com.squareup.anvil.annotations.ContributesTo
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import dev.zacsweers.catchup.app.scaffold.BuildConfig
import javax.inject.Qualifier
import kotlin.annotation.AnnotationRetention.BINARY
import timber.log.Timber

@ContributesTo(AppScope::class)
@Module
object ReleaseApplicationModule {

  @Provides
  @SingleIn(AppScope::class)
  fun provideObjectWatcher(): CatchUpObjectWatcher = CatchUpObjectWatcher.None

  @Qualifier @Retention(BINARY) private annotation class BugsnagKey

  @BugsnagKey
  @Provides
  @SingleIn(AppScope::class)
  fun provideBugsnagKey(): String = BuildConfig.BUGSNAG_KEY

  @Initializers
  @IntoSet
  @Provides
  fun bugsnagInit(application: Application, @BugsnagKey key: String): () -> Unit = {
    getOrStartBugsnag(application, key)
  }

  @IntoSet
  @Provides
  fun provideBugsnagTree(
    application: Application,
    @BugsnagKey key: String,
    lumberYard: LumberYard,
  ): Timber.Tree {
    return BugsnagTree(lumberYard).also {
      getOrStartBugsnag(application, key).addOnError { error ->
        it.update(error)
        true
      }
    }
  }

  private fun getOrStartBugsnag(application: Application, key: String): Client {
    return if (!Bugsnag.isStarted()) {
      Bugsnag.start(application, key)
    } else {
      Bugsnag.getClient()
    }
  }
}
