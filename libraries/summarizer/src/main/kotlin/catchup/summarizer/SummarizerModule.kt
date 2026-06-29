/*
 * Copyright (C) 2026. Zac Sweers
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
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
package catchup.summarizer

import catchup.appconfig.AppConfig
import catchup.libraries.retrofitconverters.delegatingCallFactory
import catchup.sqldelight.SqlDriverFactory
import catchup.util.network.AuthInterceptor
import com.squareup.moshi.Moshi
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.Qualifier
import dev.zacsweers.metro.SingleIn
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.create

@Qualifier private annotation class InternalApi

@ContributesTo(AppScope::class)
interface SummarizerModule {

  @Provides
  @SingleIn(AppScope::class)
  fun provideSummarizerDatabase(factory: SqlDriverFactory): SummarizationsDatabase =
    SummarizationsDatabase(factory.create(SummarizationsDatabase.Schema, "summarizations.db"))

  @Provides
  @InternalApi
  fun provideOpenAiOkHttpClient(client: OkHttpClient): OkHttpClient {
    return client
      .newBuilder()
      .addInterceptor(AuthInterceptor("Bearer", BuildConfig.OPEN_AI_KEY))
      .build()
  }

  @Provides
  fun chatGptApi(
    @InternalApi client: Lazy<OkHttpClient>,
    moshi: Moshi,
    appConfig: AppConfig,
  ): ChatGptApi =
    Retrofit.Builder()
      .baseUrl("https://api.openai.com")
      .delegatingCallFactory(client)
      .addConverterFactory(MoshiConverterFactory.create(moshi))
      .validateEagerly(appConfig.isDebug)
      .build()
      .create()
}
