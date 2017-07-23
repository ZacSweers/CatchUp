/*
 * Copyright (c) 2017 Zac Sweers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.sweers.catchup.data

import android.content.Context
import android.content.SharedPreferences
import com.f2prateek.rx.preferences2.RxSharedPreferences
import com.squareup.moshi.ArrayMapJsonAdapter
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import dagger.multibindings.Multibinds
import io.reactivex.schedulers.Schedulers
import io.sweers.catchup.data.adapters.ArrayCollectionJsonAdapter
import io.sweers.catchup.data.adapters.UnescapeJsonAdapter
import io.sweers.catchup.injection.qualifiers.ApplicationContext
import io.sweers.catchup.injection.qualifiers.NetworkInterceptor
import io.sweers.inspector.Inspector
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
abstract class DataModule {

  @NetworkInterceptor
  @Multibinds
  internal abstract fun provideNetworkInterceptors(): Set<Interceptor>

  @Multibinds
  internal abstract fun provideInterceptors(): Set<Interceptor>

  @Module
  companion object {

    private val HTTP_RESPONSE_CACHE = (10 * 1024 * 1024).toLong()
    private val HTTP_TIMEOUT_S = 30

    @Provides
    @JvmStatic
    @Singleton
    internal fun provideCache(@ApplicationContext context: Context): Cache {
      // Temporary pending https://github.com/apollographql/apollo-android/pull/421
      //if (Looper.myLooper() == Looper.getMainLooper()) {
      //  throw new IllegalStateException("Cache initialized on main thread.");
      //}
      return Cache(context.cacheDir, HTTP_RESPONSE_CACHE)
    }

    @Provides
    @JvmStatic
    @Singleton
    internal fun provideOkHttpClient(cache: Cache,
        interceptors: Set<@JvmSuppressWildcards Interceptor>,
        @NetworkInterceptor networkInterceptors: Set<@JvmSuppressWildcards Interceptor>): OkHttpClient {
      // Temporary pending https://github.com/apollographql/apollo-android/pull/421
      //if (Looper.myLooper() == Looper.getMainLooper()) {
      //  throw new IllegalStateException("HTTP client initialized on main thread.");
      //}

      val builder = OkHttpClient.Builder().connectTimeout(HTTP_TIMEOUT_S.toLong(), TimeUnit.SECONDS)
          .readTimeout(HTTP_TIMEOUT_S.toLong(), TimeUnit.SECONDS)
          .writeTimeout(HTTP_TIMEOUT_S.toLong(), TimeUnit.SECONDS)
          .cache(cache)

      builder.networkInterceptors()
          .addAll(networkInterceptors)
      builder.interceptors()
          .addAll(interceptors)

      return builder.build()
    }

    @Provides
    @JvmStatic
    @Singleton
    internal fun provideMoshi(): Moshi {
      return Moshi.Builder().add(ModelArbiter.createMoshiAdapterFactory())
          .add(UnescapeJsonAdapter.FACTORY)
          .add(ArrayMapJsonAdapter.FACTORY)
          .add(ArrayCollectionJsonAdapter.FACTORY)
          .build()
    }

    @Provides
    @JvmStatic
    @Singleton
    internal fun provideInspector(): Inspector {
      return Inspector.Builder()
          .add(ModelArbiter.createValidatorFactory())
          .build()
    }

    @Provides
    @JvmStatic
    @Singleton
    internal fun provideInspectorConverterFactory(inspector: Inspector): InspectorConverterFactory {
      return InspectorConverterFactory.create(inspector)
    }

    @Provides
    @JvmStatic
    @Singleton
    internal fun provideRxJavaCallAdapterFactory(): RxJava2CallAdapterFactory {
      return RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io())
    }

    @Provides
    @JvmStatic
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
      return context.getSharedPreferences("catchup", Context.MODE_PRIVATE)
    }

    @Provides
    @JvmStatic
    @Singleton
    fun provideRxSharedPreferences(sharedPreferences: SharedPreferences): RxSharedPreferences {
      return RxSharedPreferences.create(sharedPreferences)
    }
  }
}

