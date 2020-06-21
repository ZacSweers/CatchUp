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
import android.os.Build
import androidx.core.app.ActivityManagerCompat
import androidx.core.content.getSystemService
import coil.Coil
import coil.DefaultRequestOptions
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.GetRequest
import coil.request.LoadRequest
import coil.request.RequestDisposable
import coil.request.RequestResult
import coil.util.CoilUtils.createDefaultCache
import coil.util.DebugLogger
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import dagger.multibindings.IntoSet
import dagger.multibindings.Multibinds
import dev.zacsweers.catchup.appconfig.AppConfig
import dev.zacsweers.catchup.appconfig.AppConfigMetadataContributor
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
import kotlinx.coroutines.Dispatchers
import okhttp3.Cache
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlin.annotation.AnnotationRetention.BINARY

@InstallIn(ApplicationComponent::class)
@Module
abstract class ApplicationModule {

  @Qualifier
  @Retention(BINARY)
  annotation class Initializers

  @Qualifier
  @Retention(BINARY)
  annotation class AsyncInitializers

  @Qualifier
  @Retention(BINARY)
  annotation class LazyDelegate

  /**
   * Provides AppConfig metadata contributors.
   */
  @Multibinds
  abstract fun metadataContributors(): Set<AppConfigMetadataContributor>

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

  @Multibinds
  abstract fun timberTrees(): Set<Timber.Tree>

  @Binds
  @ApplicationContext
  @Singleton
  abstract fun provideApplicationContext(application: Application): Context

  @Binds
  @Singleton
  abstract fun provideUiPreferences(catchupPreferences: CatchUpPreferences): UiPreferences

  @Binds
  @Singleton
  abstract fun bindAppConfig(catchUpAppConfig: CatchUpAppConfig): AppConfig

  companion object {

    /**
     * This Context is only available for things that don't care what type of Context they need.
     *
     * Wrapped so no one can try to cast it as an Application.
     */
    @Provides
    @Singleton
    internal fun provideGeneralUseContext(@ApplicationContext appContext: Context): Context = ContextWrapper(appContext)

    @Provides
    @Singleton
    internal fun versionInfo(@ApplicationContext appContext: Context): VersionInfo = appContext.versionInfo

    @Provides
    @Singleton
    internal fun markwon(
      @LazyDelegate imageLoader: ImageLoader,
      @ApplicationContext context: Context, // TODO should use themed one from activity?
      appConfig: AppConfig
    ): Markwon {
      return Markwon.builder(context)
          .textSetter(PrecomputedTextSetterCompat.create(appConfig = appConfig))
          .usePlugins(listOf(
              MovementMethodPlugin.create(LinkTouchMovementMethod()),
              ImagesPlugin.create(),
              StrikethroughPlugin.create(),
              CoilImagesPlugin.create(context, imageLoader),
              TablePlugin.create(context),
              LinkifyPlugin.create(),
              TaskListPlugin.create(context)
        //            SyntaxHighlightPlugin.create(Prism4j(), Prism4jThemeDarkula(Color.BLACK))
          ))
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
    fun coilInit(imageLoader: ImageLoader): () -> Unit = {
      Coil.setImageLoader(imageLoader)
    }

    @Qualifier
    @Retention(BINARY)
    private annotation class CoilOkHttpStack

    @Qualifier
    @Retention(BINARY)
    annotation class IsLowRamDevice

    @IsLowRamDevice
    @Provides
    @Singleton
    fun isLowRam(@ApplicationContext context: Context): Boolean {
      // Prefer higher quality images unless we're on a low RAM device
      return context.getSystemService<ActivityManager>()?.let {
        ActivityManagerCompat.isLowRamDevice(it)
      } ?: true
    }

    @CoilOkHttpStack
    @Provides
    @Singleton
    fun coilCache(@ApplicationContext context: Context): Cache = createDefaultCache(context)

    @CoilOkHttpStack
    @Provides
    @Singleton
    fun coilHttpClient(
      okHttpClient: OkHttpClient,
      @CoilOkHttpStack cache: Cache
    ): OkHttpClient {
      return okHttpClient.newBuilder()
          .cache(cache)
          .build()
    }

    @Provides
    @LazyDelegate
    @Singleton
    fun lazyImageLoader(imageLoader: dagger.Lazy<ImageLoader>): ImageLoader {
      return object : ImageLoader {
        override val defaults: DefaultRequestOptions
          get() = imageLoader.get().defaults

        override fun clearMemory() {
          imageLoader.get().clearMemory()
        }

        override suspend fun execute(request: GetRequest): RequestResult {
          return imageLoader.get().execute(request)
        }

        override fun execute(request: LoadRequest): RequestDisposable {
          return imageLoader.get().execute(request)
        }

        override fun invalidate(key: String) {
          imageLoader.get().invalidate(key)
        }

        override fun shutdown() {
          imageLoader.get().shutdown()
        }
      }
    }

    @Provides
    @Singleton
    fun imageLoader(
      @ApplicationContext context: Context,
      @IsLowRamDevice isLowRamDevice: Boolean,
      @CoilOkHttpStack okHttpClient: dagger.Lazy<OkHttpClient>,
      appConfig: AppConfig
    ): ImageLoader {
      // TODO make this like an actual builder. But for now run works...
      return ImageLoader.Builder(context).run {
        // Coil will do lazy delegation on its own under the hood, but we
        // don't need that here because we've already made it lazy. Wish this
        // wasn't the default.
        callFactory(object : Call.Factory {
          override fun newCall(request: Request): Call {
            return okHttpClient.get().newCall(request)
          }
        })

        if (appConfig.isDebug) {
          logger(DebugLogger())
        }

        // Hardware bitmaps don't work with the saturation effect
        allowHardware(false)
        allowRgb565(isLowRamDevice)
        crossfade(300)

        componentRegistry {
          if (Build.VERSION.SDK_INT >= 28) {
            add(ImageDecoderDecoder())
          } else {
            add(GifDecoder())
          }
        }

        build()
      }
    }
  }
}
