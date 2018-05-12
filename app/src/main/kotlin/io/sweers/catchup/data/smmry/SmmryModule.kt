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

import com.squareup.moshi.Moshi
import dagger.Lazy
import dagger.Module
import dagger.Provides
import io.sweers.catchup.BuildConfig
import io.sweers.catchup.data.smmry.model.SmmryResponseFactory
import io.sweers.catchup.injection.scopes.PerController
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Qualifier

@Module
abstract class SmmryModule {

  @Qualifier
  private annotation class InternalApi

  @Module
  companion object {

    @Provides
    @JvmStatic
    @InternalApi
    @PerController
    internal fun provideSmmryMoshi(moshi: Moshi): Moshi {
      return moshi.newBuilder()
          .add(SmmryResponseFactory.getInstance())
          .build()
    }

    @Provides
    @JvmStatic
    @PerController
    internal fun provideSmmryService(client: Lazy<OkHttpClient>,
        @InternalApi moshi: Moshi,
        rxJavaCallAdapterFactory: RxJava2CallAdapterFactory): SmmryService {
      return Retrofit.Builder().baseUrl(SmmryService.ENDPOINT)
          .callFactory { request ->
            client.get()
                .newCall(request)
          }
          .addCallAdapterFactory(rxJavaCallAdapterFactory)
          .addConverterFactory(MoshiConverterFactory.create(moshi))
          .validateEagerly(BuildConfig.DEBUG)
          .build()
          .create(SmmryService::class.java)
    }
  }
}
