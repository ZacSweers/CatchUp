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
package catchup.app.data

import android.content.Context
import android.os.Looper
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import catchup.appconfig.AppConfig
import catchup.di.FakeMode
import catchup.sqldelight.SqlDriverFactory
import catchup.util.data.adapters.UnescapeJsonAdapter
import catchup.util.injection.qualifiers.ApplicationContext
import catchup.util.injection.qualifiers.NetworkInterceptor
import com.jakewharton.shimo.ObjectOrderRandomizer
import com.serjltt.moshi.adapters.Wrapped
import com.squareup.moshi.Moshi
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Multibinds
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import java.util.concurrent.TimeUnit
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.OkHttpClient

@ContributesTo(AppScope::class)
interface DataModule {

  // May be empty in release builds
  @NetworkInterceptor
  @Multibinds(allowEmpty = true)
  fun provideNetworkInterceptors(): Set<Interceptor>

  @Multibinds(allowEmpty = true) fun provideInterceptors(): Set<Interceptor>

  companion object {

    private const val HTTP_RESPONSE_CACHE = (10 * 1024 * 1024).toLong()
    private const val HTTP_TIMEOUT_S = 30

    @Provides
    @SingleIn(AppScope::class)
    fun provideCache(@ApplicationContext context: Context): Cache {
      if (Looper.myLooper() == Looper.getMainLooper()) {
        throw IllegalStateException("Cache initialized on main thread.")
      }
      return Cache(context.cacheDir, HTTP_RESPONSE_CACHE)
    }

    @Provides
    @SingleIn(AppScope::class)
    fun provideOkHttpClient(
      cache: Cache,
      interceptors: Set<Interceptor>,
      @NetworkInterceptor networkInterceptors: Set<Interceptor>,
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
    fun provideMoshi(appConfig: AppConfig): Moshi {
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

    @Provides
    fun provideSqlDriverFactory(
      @ApplicationContext context: Context,
      @FakeMode isFakeMode: Boolean,
    ): SqlDriverFactory = SqlDriverFactory { schema, name ->
      AndroidSqliteDriver(schema, context, name.takeUnless { isFakeMode })
    }
  }
}
