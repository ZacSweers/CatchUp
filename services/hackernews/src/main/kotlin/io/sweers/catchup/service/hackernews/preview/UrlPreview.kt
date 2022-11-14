/*
 * Copyright (C) 2020. Zac Sweers
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
package io.sweers.catchup.service.hackernews.preview

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import dagger.Lazy
import dagger.Module
import dagger.Provides
import dev.zacsweers.catchup.appconfig.AppConfig
import io.sweers.catchup.libraries.retrofitconverters.delegatingCallFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface UrlPreview {
  @GET("/")
  suspend fun previewUrl(@Query("key") key: String, @Query("q") query: String): UrlPreviewResponse
}

@JsonClass(generateAdapter = true)
data class UrlPreviewResponse(
  val title: String,
  val description: String,
  val image: String,
  val url: String
)

@Module
object UrlPreviewModule {
  @Provides
  internal fun provideUrlPreviewService(
    client: Lazy<OkHttpClient>,
    moshi: Moshi,
    appConfig: AppConfig
  ): UrlPreview {
    val retrofit =
      Retrofit.Builder()
        .baseUrl("https://api.linkpreview.net")
        .delegatingCallFactory(client)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .validateEagerly(appConfig.isDebug)
        .build()
    return retrofit.create(UrlPreview::class.java)
  }
}
