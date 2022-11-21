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

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.os.Build.VERSION
import androidx.core.app.ActivityManagerCompat
import androidx.core.content.getSystemService
import coil.Coil
import coil.ComponentRegistry
import coil.ImageLoader
import coil.annotation.ExperimentalCoilApi
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.DefaultRequestOptions
import coil.request.Disposable
import coil.request.ImageRequest
import coil.request.ImageResult
import coil.util.DebugLogger
import com.squareup.anvil.annotations.ContributesTo
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import dagger.multibindings.Multibinds
import dev.zacsweers.catchup.appconfig.AppConfig
import dev.zacsweers.catchup.appconfig.AppConfigMetadataContributor
import dev.zacsweers.catchup.di.AppScope
import dev.zacsweers.catchup.di.SingleIn
import io.noties.markwon.Markwon
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tasklist.TaskListPlugin
import io.noties.markwon.image.ImagesPlugin
import io.noties.markwon.image.coil.CoilImagesPlugin
import io.noties.markwon.linkify.LinkifyPlugin
import io.noties.markwon.movement.MovementMethodPlugin
import io.sweers.catchup.CatchUpPreferences
import io.sweers.catchup.base.ui.UiPreferences
import io.sweers.catchup.base.ui.VersionInfo
import io.sweers.catchup.base.ui.versionInfo
import io.sweers.catchup.util.LinkTouchMovementMethod
import io.sweers.catchup.util.PrecomputedTextSetterCompat
import io.sweers.catchup.util.injection.qualifiers.ApplicationContext
import javax.inject.Qualifier
import kotlin.annotation.AnnotationRetention.BINARY
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient
import timber.log.Timber

@ContributesTo(AppScope::class)
@Module
abstract class ApplicationModule {

  @Qualifier @Retention(BINARY) annotation class Initializers

  @Qualifier @Retention(BINARY) annotation class AsyncInitializers

  @Qualifier @Retention(BINARY) annotation class LazyDelegate

  /** Provides AppConfig metadata contributors. */
  @Multibinds abstract fun metadataContributors(): Set<AppConfigMetadataContributor>

  /** Provides initializers for app startup. */
  @Initializers @Multibinds abstract fun initializers(): Set<() -> Unit>

  /** Provides initializers for app startup that can be initialized async. */
  @AsyncInitializers @Multibinds abstract fun asyncInitializers(): Set<() -> Unit>

  @Multibinds abstract fun timberTrees(): Set<Timber.Tree>

  @Binds
  @ApplicationContext
  @SingleIn(AppScope::class)
  abstract fun Application.provideApplicationContext(): Context

  @Binds
  @SingleIn(AppScope::class)
  abstract fun CatchUpPreferences.provideUiPreferences(): UiPreferences

  @Binds @SingleIn(AppScope::class) abstract fun CatchUpAppConfig.bindAppConfig(): AppConfig

  companion object {

    /**
     * This Context is only available for things that don't care what type of Context they need.
     *
     * Wrapped so no one can try to cast it as an Application.
     */
    @Provides
    @SingleIn(AppScope::class)
    internal fun @receiver:ApplicationContext Context.provideGeneralUseContext(): Context =
      ContextWrapper(this)

    @Provides
    @SingleIn(AppScope::class)
    internal fun @receiver:ApplicationContext Context.versionInfo(): VersionInfo = versionInfo

    @Provides
    @SingleIn(AppScope::class)
    internal fun markwon(
      @LazyDelegate imageLoader: ImageLoader,
      @ApplicationContext context: Context, // TODO should use themed one from activity?
      appConfig: AppConfig
    ): Markwon {
      return Markwon.builder(context)
        .textSetter(PrecomputedTextSetterCompat.create(appConfig = appConfig))
        .usePlugins(
          listOf(
            MovementMethodPlugin.create(LinkTouchMovementMethod()),
            ImagesPlugin.create(),
            StrikethroughPlugin.create(),
            CoilImagesPlugin.create(context, imageLoader),
            TablePlugin.create(context),
            LinkifyPlugin.create(),
            TaskListPlugin.create(context)
            //            SyntaxHighlightPlugin.create(Prism4j(), Prism4jThemeDarkula(Color.BLACK))
            )
        )
        .build()
    }

    @AsyncInitializers
    @IntoSet
    @Provides
    fun mainDispatcherInit(): () -> Unit = {
      // This makes a call to disk, so initialize it off the main thread first... ironically
      Dispatchers.Main
    }

    @Initializers
    @IntoSet
    @Provides
    fun coilInit(imageLoader: ImageLoader): () -> Unit = { Coil.setImageLoader(imageLoader) }

    @Qualifier @Retention(BINARY) private annotation class CoilOkHttpStack

    @Qualifier @Retention(BINARY) annotation class IsLowRamDevice

    @IsLowRamDevice
    @Provides
    @SingleIn(AppScope::class)
    fun isLowRam(@ApplicationContext context: Context): Boolean {
      // Prefer higher quality images unless we're on a low RAM device
      return context.getSystemService<ActivityManager>()?.let {
        ActivityManagerCompat.isLowRamDevice(it)
      }
        ?: true
    }

    @ExperimentalCoilApi
    @Provides
    @LazyDelegate
    @SingleIn(AppScope::class)
    fun lazyImageLoader(imageLoader: dagger.Lazy<ImageLoader>): ImageLoader {
      return object : ImageLoader {
        override val components: ComponentRegistry
          get() = imageLoader.get().components
        override val defaults: DefaultRequestOptions
          get() = imageLoader.get().defaults
        override val diskCache: DiskCache?
          get() = imageLoader.get().diskCache
        override val memoryCache: MemoryCache?
          get() = imageLoader.get().memoryCache

        override fun enqueue(request: ImageRequest): Disposable {
          return imageLoader.get().enqueue(request)
        }

        override suspend fun execute(request: ImageRequest): ImageResult {
          return imageLoader.get().execute(request)
        }

        override fun newBuilder(): ImageLoader.Builder {
          return imageLoader.get().newBuilder()
        }

        override fun shutdown() {
          imageLoader.get().shutdown()
        }
      }
    }

    @Provides
    @SingleIn(AppScope::class)
    fun imageLoader(
      @ApplicationContext context: Context,
      @IsLowRamDevice isLowRamDevice: Boolean,
      okHttpClient: dagger.Lazy<OkHttpClient>,
      appConfig: AppConfig
    ): ImageLoader {
      // TODO make this like an actual builder. But for now run works...
      return ImageLoader.Builder(context).run {
        // Coil will do lazy delegation on its own under the hood, but we
        // don't need that here because we've already made it lazy. Wish this
        // wasn't the default.
        callFactory { request -> okHttpClient.get().newCall(request) }

        if (appConfig.isDebug) {
          logger(DebugLogger())
        }

        // Hardware bitmaps don't work with the saturation effect
        allowHardware(false)
        allowRgb565(isLowRamDevice)
        crossfade(300)

        components {
          add(
            if (VERSION.SDK_INT >= 28) {
              ImageDecoderDecoder.Factory()
            } else {
              GifDecoder.Factory()
            }
          )
        }

        build()
      }
    }
  }
}
