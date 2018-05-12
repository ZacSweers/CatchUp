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
import com.facebook.stetho.okhttp3.StethoInterceptor
import com.readystatesoftware.chuck.ChuckInterceptor
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import io.sweers.catchup.util.injection.qualifiers.ApplicationContext
import io.sweers.catchup.util.injection.qualifiers.NetworkInterceptor
import okhttp3.Interceptor
import okhttp3.logging.HttpLoggingInterceptor
import timber.log.Timber
import javax.inject.Singleton

@Module
object VariantDataModule {

  @Provides
  @NetworkInterceptor
  @IntoSet
  @JvmStatic
  @Singleton
  internal fun provideLoggingInterceptor(): Interceptor {
    val loggingInterceptor = HttpLoggingInterceptor { message ->
      Timber.tag("OkHttp")
          .v(message)
    }
    loggingInterceptor.level = HttpLoggingInterceptor.Level.BASIC
    return loggingInterceptor
  }

  @Provides
  @NetworkInterceptor
  @IntoSet
  @JvmStatic
  @Singleton
  internal fun provideStethoInterceptor(): Interceptor = StethoInterceptor()

  @Provides
  @NetworkInterceptor
  @IntoSet
  @JvmStatic
  @Singleton
  internal fun provideChuckInterceptor(@ApplicationContext context: Context): Interceptor =
      ChuckInterceptor(context)

  @Provides
  @IntoSet
  @JvmStatic
  @Singleton
  internal fun provideMockDataInterceptor(@ApplicationContext context: Context): Interceptor =
      MockDataInterceptor(context)
}
