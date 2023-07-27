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
package io.sweers.catchup.data

import android.content.Context
import android.os.Looper
import com.jakewharton.shimo.ObjectOrderRandomizer
import com.serjltt.moshi.adapters.Wrapped
import com.squareup.anvil.annotations.ContributesTo
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import dagger.multibindings.Multibinds
import dev.zacsweers.catchup.appconfig.AppConfig
import dev.zacsweers.catchup.di.AppScope
import dev.zacsweers.catchup.di.SingleIn
import io.sweers.catchup.injection.DaggerSet
import io.sweers.catchup.util.data.adapters.UnescapeJsonAdapter
import io.sweers.catchup.util.injection.qualifiers.ApplicationContext
import io.sweers.catchup.util.injection.qualifiers.NetworkInterceptor
import java.util.concurrent.TimeUnit
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.OkHttpClient

@ContributesTo(AppScope::class)
@Module
abstract class DataModule {

  @NetworkInterceptor
  @Multibinds
  internal abstract fun provideNetworkInterceptors(): Set<Interceptor>

  @Multibinds internal abstract fun provideInterceptors(): Set<Interceptor>

  companion object {

    private const val HTTP_RESPONSE_CACHE = (10 * 1024 * 1024).toLong()
    private const val HTTP_TIMEOUT_S = 30

    @Provides
    @SingleIn(AppScope::class)
    internal fun provideCache(@ApplicationContext context: Context): Cache {
      if (Looper.myLooper() == Looper.getMainLooper()) {
        throw IllegalStateException("Cache initialized on main thread.")
      }
      return Cache(context.cacheDir, HTTP_RESPONSE_CACHE)
    }

    @Provides
    @SingleIn(AppScope::class)
    internal fun provideOkHttpClient(
      cache: Cache,
      interceptors: DaggerSet<Interceptor>,
      @NetworkInterceptor networkInterceptors: DaggerSet<Interceptor>
    ): OkHttpClient {
      if (Looper.myLooper() == Looper.getMainLooper()) {
        throw IllegalStateException("HTTP client initialized on main thread.")
      }

      val builder =
        OkHttpClient.Builder()
          .connectTimeout(HTTP_TIMEOUT_S.toLong(), TimeUnit.SECONDS)
          .readTimeout(HTTP_TIMEOUT_S.toLong(), TimeUnit.SECONDS)
          .writeTimeout(HTTP_TIMEOUT_S.toLong(), TimeUnit.SECONDS)
          .cache(cache)

      builder.networkInterceptors().addAll(networkInterceptors)
      builder.interceptors().addAll(interceptors)

      return builder.build()
    }

    @Provides
    @SingleIn(AppScope::class)
    internal fun provideMoshi(appConfig: AppConfig): Moshi {
      return Moshi.Builder()
        .apply {
          // TODO would like to just have this in debug but need to abstract it better
          if (appConfig.isDebug) {
            add(ObjectOrderRandomizer.create())
          }
        }
        .add(Wrapped.ADAPTER_FACTORY)
        .add(UnescapeJsonAdapter.FACTORY)
        .build()
    }
  }
}
