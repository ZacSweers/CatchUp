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

package io.sweers.catchup.data.smmry

import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import com.squareup.moshi.Moshi
import dagger.Lazy
import dagger.Module
import dagger.Provides
import io.sweers.catchup.BuildConfig
import io.sweers.catchup.data.CatchUpDatabase
import io.sweers.catchup.data.smmry.model.SmmryResponseFactory
import io.sweers.catchup.injection.scopes.PerFragment
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Qualifier

@Module
object SmmryModule {

  @Qualifier
  annotation class ForSmmry

  @Provides
  @JvmStatic
  @ForSmmry
  @PerFragment
  internal fun provideSmmryMoshi(moshi: Moshi): Moshi {
    return moshi.newBuilder()
        .add(SmmryResponseFactory.getInstance())
        .build()
  }

  @Provides
  @JvmStatic
  @PerFragment
  internal fun provideSmmryService(client: Lazy<OkHttpClient>,
      @ForSmmry moshi: Moshi): SmmryService {
    return Retrofit.Builder().baseUrl(SmmryService.ENDPOINT)
        .callFactory { request ->
          client.get()
              .newCall(request)
        }
        .addCallAdapterFactory(CoroutineCallAdapterFactory())
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .validateEagerly(BuildConfig.DEBUG)
        .build()
        .create(SmmryService::class.java)
  }

  @Provides
  @JvmStatic
  @PerFragment
  internal fun provideServiceDao(catchUpDatabase: CatchUpDatabase) = catchUpDatabase.smmryDao()
}
