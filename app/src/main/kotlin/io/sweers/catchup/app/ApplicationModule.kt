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
import android.content.Context
import android.content.ContextWrapper
import com.gabrielittner.threetenbp.LazyThreeTen
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import dagger.multibindings.Multibinds
import io.noties.markwon.Markwon
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tasklist.TaskListPlugin
import io.noties.markwon.image.ImagesPlugin
import io.noties.markwon.image.glide.GlideImagesPlugin
import io.noties.markwon.linkify.LinkifyPlugin
import io.noties.markwon.movement.MovementMethodPlugin
import io.sweers.catchup.CatchUpPreferences
import io.sweers.catchup.base.ui.UiPreferences
import io.sweers.catchup.data.InstanceBasedOkHttpLibraryGlideModule
import io.sweers.catchup.util.LinkTouchMovementMethod
import io.sweers.catchup.util.PrecomputedTextSetterCompat
import io.sweers.catchup.util.injection.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import timber.log.Timber
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlin.annotation.AnnotationRetention.BINARY

@Module(subcomponents = [InstanceBasedOkHttpLibraryGlideModule.Component::class])
abstract class ApplicationModule {

  @Qualifier
  @Retention(BINARY)
  annotation class Initializers

  @Qualifier
  @Retention(BINARY)
  annotation class AsyncInitializers

  /**
   * Provides initializers for app startup.
   */
  @Initializers
  @Multibinds
  abstract fun initializers(): Set<() -> Unit>

  /**
   * Provides initializers for app startup that can be initialized async.
   */
  @AsyncInitializers
  @Multibinds
  abstract fun asyncInitializers(): Set<() -> Unit>

  @Initializers
  @Multibinds
  abstract fun timberTrees(): Set<Timber.Tree>

  @Binds
  @ApplicationContext
  @Singleton
  abstract fun provideApplicationContext(application: Application): Context

  @Binds
  @Singleton
  abstract fun provideUiPreferences(catchupPreferences: CatchUpPreferences): UiPreferences

  @Module
  companion object {

    /**
     * This Context is only available for things that don't care what type of Context they need.
     *
     * Wrapped so no one can try to cast it as an Application.
     */
    @Provides
    @JvmStatic
    @Singleton
    internal fun provideGeneralUseContext(@ApplicationContext appContext: Context): Context = ContextWrapper(appContext)

    @Provides
    @JvmStatic
    @Singleton
    internal fun markwon(
      @ApplicationContext context: Context // TODO should use themed one from activity?
    ): Markwon {
      return Markwon.builder(context)
          .textSetter(PrecomputedTextSetterCompat.create())
          .usePlugins(listOf(
              MovementMethodPlugin.create(LinkTouchMovementMethod()),
              ImagesPlugin.create(),
              StrikethroughPlugin.create(),
              GlideImagesPlugin.create(context),
              TablePlugin.create(context),
              LinkifyPlugin.create(),
              TaskListPlugin.create(context)
        //            SyntaxHighlightPlugin.create(Prism4j(), Prism4jThemeDarkula(Color.BLACK))
          ))
          .build()
        }

    @AsyncInitializers
    @JvmStatic
    @IntoSet
    @Provides
    fun threeTenInit(application: Application): () -> Unit = {
      LazyThreeTen.init(application)
      LazyThreeTen.cacheZones()
    }

    @AsyncInitializers
    @JvmStatic
    @IntoSet
    @Provides
    fun mainDispatcherInit(): () -> Unit = {
      // This makes a call to disk, so initialize it off the main thread first... ironically
      Dispatchers.Main
    }
  }
}
