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
import androidx.core.app.ActivityManagerCompat
import androidx.core.content.getSystemService
import coil.Coil
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.util.CoilLogger
import coil.util.CoilUtils.createDefaultCache
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
import io.sweers.catchup.BuildConfig
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

@Module
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
    internal fun versionInfo(@ApplicationContext appContext: Context): VersionInfo = appContext.versionInfo

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

    @Initializers
    @JvmStatic
    @IntoSet
    @Provides
    fun coilInit(imageLoader: ImageLoader): () -> Unit = {
      Coil.setDefaultImageLoader(imageLoader)
      CoilLogger.setEnabled(BuildConfig.DEBUG)
    }

    @Qualifier
    @Retention(BINARY)
    private annotation class CoilOkHttpStack

    @CoilOkHttpStack
    @Singleton
    @JvmStatic
    @Provides
    fun coilCache(@ApplicationContext context: Context): Cache = createDefaultCache(context)

    @CoilOkHttpStack
    @Singleton
    @JvmStatic
    @Provides
    fun coilHttpClient(
      okHttpClient: OkHttpClient,
      @CoilOkHttpStack cache: Cache
    ): OkHttpClient {
      return okHttpClient.newBuilder()
          .cache(cache)
          .build()
    }

    @Singleton
    @JvmStatic
    @Provides
    fun imageLoader(
      @ApplicationContext context: Context,
      @CoilOkHttpStack okHttpClient: dagger.Lazy<OkHttpClient>
    ): ImageLoader {
      return ImageLoader(context) {
        // Coil will do lazy delegation on its own under the hood, but we
        // don't need that here because we've already made it lazy. Wish this
        // wasn't the default.
        callFactory(object : Call.Factory {
          override fun newCall(request: Request): Call {
            return okHttpClient.get().newCall(request)
          }
        })

        // Hardware bitmaps don't work with the saturation effect
        allowHardware(false)

        // TODO move this to qualified dependency
        // Prefer higher quality images unless we're on a low RAM device
        val isLowRamDevice = context.getSystemService<ActivityManager>()?.let {
          ActivityManagerCompat.isLowRamDevice(it)
        } ?: true
        allowRgb565(isLowRamDevice)
        crossfade(true)
        crossfade(300)

        componentRegistry {
          // TODO re-enable native decoder once https://github.com/coil-kt/coil/issues/101 is fixed
//          if (Build.VERSION.SDK_INT >= 28) {
//            add(ImageDecoderDecoder())
//          } else {
//            add(GifDecoder())
//          }
          add(GifDecoder())
        }
      }
    }
  }
}
