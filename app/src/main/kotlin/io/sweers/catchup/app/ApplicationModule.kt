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
import io.noties.markwon.Markwon
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tasklist.TaskListPlugin
import io.noties.markwon.image.ImagesPlugin
import io.noties.markwon.image.glide.GlideImagesPlugin
import io.noties.markwon.linkify.LinkifyPlugin
import io.noties.markwon.movement.MovementMethodPlugin
import io.sweers.catchup.util.LinkTouchMovementMethod
import io.sweers.catchup.util.injection.qualifiers.ApplicationContext
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
      @ApplicationContext context: Context // TODO should use themed one from activity?
    ): Markwon {
      return Markwon.builder(context)
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
  }
}
