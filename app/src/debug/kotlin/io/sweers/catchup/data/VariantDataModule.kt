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
import com.squareup.anvil.annotations.ContributesTo
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import dev.zacsweers.catchup.di.AppScope
import dev.zacsweers.catchup.di.SingleIn
import io.sweers.catchup.util.injection.qualifiers.ApplicationContext
import io.sweers.catchup.util.injection.qualifiers.NetworkInterceptor
import okhttp3.Interceptor
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.Level.BASIC
import timber.log.Timber

private inline fun httpLoggingInterceptor(
  level: HttpLoggingInterceptor.Level = HttpLoggingInterceptor.Level.NONE,
  crossinline logger: (String) -> Unit
): HttpLoggingInterceptor {
  return HttpLoggingInterceptor { message -> logger(message) }.also { it.level = level }
}

@ContributesTo(AppScope::class)
@Module
object VariantDataModule {

  @SingleIn(AppScope::class)
  @Provides
  @NetworkInterceptor
  @IntoSet
  internal fun provideLoggingInterceptor(): Interceptor =
    httpLoggingInterceptor(BASIC) { message -> Timber.tag("OkHttp").v(message) }

  //  @Provides
  //  @SingleIn(AppScope::class)
  //  @NetworkInterceptor
  //  @IntoSet
  //  internal fun provideChuckInterceptor(@ApplicationContext context: Context): Interceptor =
  //      ChuckInterceptor(context)

  @SingleIn(AppScope::class)
  @Provides
  @IntoSet
  internal fun provideMockDataInterceptor(
    @ApplicationContext context: Context,
    debugPreferences: DebugPreferences
  ): Interceptor = MockDataInterceptor(context, debugPreferences)
}
