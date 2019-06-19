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
import dagger.Binds
import dagger.Module
import dagger.Provides
import io.sweers.catchup.util.LinkTouchMovementMethod
import io.sweers.catchup.util.injection.qualifiers.ApplicationContext
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import ru.noties.markwon.Markwon
import ru.noties.markwon.core.CorePlugin
import ru.noties.markwon.ext.strikethrough.StrikethroughPlugin
import ru.noties.markwon.ext.tables.TablePlugin
import ru.noties.markwon.ext.tasklist.TaskListPlugin
import ru.noties.markwon.image.ImagesPlugin
import ru.noties.markwon.image.gif.GifPlugin
import ru.noties.markwon.image.okhttp.OkHttpImagesPlugin
import ru.noties.markwon.movement.MovementMethodPlugin
import javax.inject.Singleton

@Module
abstract class ApplicationModule {

  @Binds
  @ApplicationContext
  @Singleton
  abstract fun provideApplicationContext(application: Application): Context

  @Module
  companion object {

    @Provides
    @JvmStatic
    @Singleton
    internal fun markwon(
        @ApplicationContext context: Context, // TODO should use themed one from activity?
        okhttpClient: dagger.Lazy<OkHttpClient>
    ): Markwon {
      return Markwon.builder(context)
          .usePlugins(listOf(
              CorePlugin.create(),
              MovementMethodPlugin.create(LinkTouchMovementMethod()),
              ImagesPlugin.create(context),
              StrikethroughPlugin.create(),
              GifPlugin.create(),
              TablePlugin.create(context),
              TaskListPlugin.create(context),
              OkHttpImagesPlugin.create(object : Call.Factory {
                override fun newCall(request: Request): Call {
                  return okhttpClient.get().newCall(request)
                }
              })
        //            SyntaxHighlightPlugin.create(Prism4j(), Prism4jThemeDarkula(Color.BLACK))
          ))
          .build()
        }
  }
}
