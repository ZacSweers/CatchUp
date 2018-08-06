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

package io.sweers.catchup.data

import android.content.Context
import android.content.SharedPreferences
import android.os.Looper
import com.f2prateek.rx.preferences2.RxSharedPreferences
import com.serjltt.moshi.adapters.Wrapped
import com.squareup.moshi.ArrayMapJsonAdapter
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import dagger.multibindings.Multibinds
import io.reactivex.schedulers.Schedulers
import io.sweers.catchup.data.adapters.ArrayCollectionJsonAdapter
import io.sweers.catchup.gemoji.GemojiModule
import io.sweers.catchup.injection.SharedPreferencesName
import io.sweers.catchup.util.data.adapters.UnescapeJsonAdapter
import io.sweers.catchup.util.injection.qualifiers.ApplicationContext
import io.sweers.catchup.util.injection.qualifiers.NetworkInterceptor
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module(includes = [GithubApolloModule::class, GemojiModule::class])
abstract class DataModule {

  @NetworkInterceptor
  @Multibinds
  internal abstract fun provideNetworkInterceptors(): Set<Interceptor>

  @Multibinds
  internal abstract fun provideInterceptors(): Set<Interceptor>

  @Module
  companion object {

    private const val HTTP_RESPONSE_CACHE = (10 * 1024 * 1024).toLong()
    private const val HTTP_TIMEOUT_S = 30

    @Provides
    @JvmStatic
    @Singleton
    internal fun provideCache(@ApplicationContext context: Context): Cache {
      if (Looper.myLooper() == Looper.getMainLooper()) {
        throw IllegalStateException("Cache initialized on main thread.")
      }
      return Cache(context.cacheDir, HTTP_RESPONSE_CACHE)
    }

    @Provides
    @JvmStatic
    @Singleton
    internal fun provideOkHttpClient(cache: Cache,
        interceptors: Set<@JvmSuppressWildcards Interceptor>,
        @NetworkInterceptor networkInterceptors: Set<@JvmSuppressWildcards Interceptor>): OkHttpClient {
      if (Looper.myLooper() == Looper.getMainLooper()) {
        throw IllegalStateException("HTTP client initialized on main thread.")
      }

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
      return Moshi.Builder()
          .add(Wrapped.ADAPTER_FACTORY)
          .add(UnescapeJsonAdapter.FACTORY)
          .add(ArrayMapJsonAdapter.FACTORY)
          .add(ArrayCollectionJsonAdapter.FACTORY)
          .build()
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
    @SharedPreferencesName
    fun provideSharedPreferencesName(): String {
      return "catchup"
    }

    @Provides
    @JvmStatic
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context, @SharedPreferencesName name: String): SharedPreferences {
      return context.getSharedPreferences(name, Context.MODE_PRIVATE)
    }

    @Provides
    @JvmStatic
    @Singleton
    fun provideRxSharedPreferences(sharedPreferences: SharedPreferences): RxSharedPreferences {
      return RxSharedPreferences.create(sharedPreferences)
    }

    @Provides
    @JvmStatic
    @Singleton
    internal fun provideCatchUpDatabase(@ApplicationContext context: Context): CatchUpDatabase {
      return CatchUpDatabase.getDatabase(context)
    }

    @Provides
    @JvmStatic
    @Singleton
    internal fun provideServiceDao(catchUpDatabase: CatchUpDatabase): ServiceDao {
      return catchUpDatabase.serviceDao()
    }
  }
}

