/*
 * Copyright (c) 2018 Zac Sweers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.sweers.catchup.flipper

import android.content.Context
import com.facebook.flipper.core.FlipperPlugin
import com.facebook.flipper.plugins.inspector.DescriptorMapping
import com.facebook.flipper.plugins.inspector.InspectorFlipperPlugin
import com.facebook.flipper.plugins.network.FlipperOkhttpInterceptor
import com.facebook.flipper.plugins.network.NetworkFlipperPlugin
import com.facebook.flipper.plugins.sharedpreferences.SharedPreferencesFlipperPlugin
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import dagger.multibindings.Multibinds
import io.sweers.catchup.injection.SharedPreferencesName
import io.sweers.catchup.util.injection.qualifiers.ApplicationContext
import io.sweers.catchup.util.injection.qualifiers.NetworkInterceptor
import okhttp3.Interceptor
import javax.inject.Singleton

@Module
abstract class FlipperModule {

  @Multibinds
  @Singleton
  abstract fun provideFlipperPlugins(): Set<FlipperPlugin>

  @Binds
  @IntoSet
  @Singleton
  abstract fun provideNetworkFlipperPluginIntoSet(plugin: NetworkFlipperPlugin): FlipperPlugin

  @Module
  companion object {

    @IntoSet
    @JvmStatic
    @Provides
    @Singleton
    fun provideSharedPreferencesPlugin(@ApplicationContext context: Context,
        @SharedPreferencesName preferencesName: String): FlipperPlugin {
      return SharedPreferencesFlipperPlugin(context, preferencesName)
    }

    @IntoSet
    @JvmStatic
    @Provides
    @Singleton
    fun provideViewInspectorPlugin(@ApplicationContext context: Context): FlipperPlugin {
      return InspectorFlipperPlugin(context, DescriptorMapping.withDefaults())
    }

    @JvmStatic
    @Provides
    @Singleton
    fun provideOkHttpInspectorPlugin(): NetworkFlipperPlugin {
      return NetworkFlipperPlugin()
    }

    @IntoSet
    @JvmStatic
    @NetworkInterceptor
    @Provides
    @Singleton
    fun provideOkHttpInspectorPluginInterceptor(plugin: NetworkFlipperPlugin): Interceptor {
      return FlipperOkhttpInterceptor(plugin)
    }

  }

}
