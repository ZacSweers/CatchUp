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
package catchup.app.flipper

import android.content.Context
import catchup.di.AppScope
import catchup.di.SingleIn
import catchup.util.injection.qualifiers.ApplicationContext
import catchup.util.injection.qualifiers.NetworkInterceptor
import com.facebook.flipper.core.FlipperPlugin
import com.facebook.flipper.plugins.crashreporter.CrashReporterPlugin
import com.facebook.flipper.plugins.databases.DatabasesFlipperPlugin
import com.facebook.flipper.plugins.inspector.DescriptorMapping
import com.facebook.flipper.plugins.inspector.InspectorFlipperPlugin
import com.facebook.flipper.plugins.network.FlipperOkhttpInterceptor
import com.facebook.flipper.plugins.network.NetworkFlipperPlugin
import com.squareup.anvil.annotations.ContributesTo
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import dagger.multibindings.Multibinds
import okhttp3.Interceptor

@ContributesTo(AppScope::class)
@Module
abstract class FlipperModule {

  @Multibinds abstract fun provideFlipperPlugins(): Set<FlipperPlugin>

  @Binds
  @IntoSet
  @SingleIn(AppScope::class)
  abstract fun provideNetworkFlipperPluginIntoSet(plugin: NetworkFlipperPlugin): FlipperPlugin

  companion object {
    @IntoSet
    @Provides
    fun provideViewInspectorPlugin(@ApplicationContext context: Context): FlipperPlugin {
      return InspectorFlipperPlugin(context, DescriptorMapping.withDefaults())
    }

    @Provides
    @SingleIn(AppScope::class)
    fun provideOkHttpInspectorPlugin(): NetworkFlipperPlugin {
      return NetworkFlipperPlugin()
    }

    // TODO This should go at the end of the list. We can try to differentiate these by wrapping
    // them
    //  in a "ReadOnlyInterceptor" type that we sort at the interceptor injection site
    @IntoSet
    @NetworkInterceptor
    @Provides
    fun provideOkHttpInspectorPluginInterceptor(plugin: NetworkFlipperPlugin): Interceptor {
      return FlipperOkhttpInterceptor(plugin)
    }

    @IntoSet
    @Provides
    fun provideFlipperDatabasesPlugin(@ApplicationContext context: Context): FlipperPlugin {
      return DatabasesFlipperPlugin(context)
    }

    @Provides
    @SingleIn(AppScope::class)
    fun provideFlipperCrashReporterPlugin(): CrashReporterPlugin = CrashReporterPlugin.getInstance()
  }
}
