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
package catchup.app.ui.bugreport

import catchup.appconfig.AppConfig
import catchup.di.AppScope
import catchup.di.SingleIn
import catchup.libraries.retrofitconverters.delegatingCallFactory
import com.serjltt.moshi.adapters.Wrapped
import com.squareup.anvil.annotations.ContributesTo
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import dagger.Lazy
import dagger.Module
import dagger.Provides
import dev.zacsweers.catchup.app.scaffold.BuildConfig
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ImgurUploadApi {
  @Multipart
  @Headers("Authorization: Client-ID ${BuildConfig.IMGUR_CLIENT_ACCESS_TOKEN}")
  @POST("image")
  @Wrapped(path = ["data", "link"])
  suspend fun postImage(@Part file: MultipartBody.Part): String
}

interface GitHubIssueApi {
  @Headers(
    value =
      [
        "Authorization: token ${BuildConfig.GITHUB_DEVELOPER_TOKEN}",
        "Accept: application/vnd.github.v3+json",
      ]
  )
  @POST("repos/zacsweers/catchup/issues")
  @Wrapped(path = ["html_url"])
  suspend fun createIssue(@Body issue: GitHubIssue): String
}

@JsonClass(generateAdapter = true) data class GitHubIssue(val title: String, val body: String)

@ContributesTo(AppScope::class)
@Module
object BugReportModule {

  @SingleIn(AppScope::class)
  @Provides
  internal fun provideImgurService(
    client: Lazy<OkHttpClient>,
    moshi: Moshi,
    appConfig: AppConfig,
  ): ImgurUploadApi {
    return Retrofit.Builder()
      .baseUrl("https://api.imgur.com/3/")
      .delegatingCallFactory(client)
      .addConverterFactory(
        MoshiConverterFactory.create(moshi.newBuilder().add(Wrapped.ADAPTER_FACTORY).build())
      )
      .validateEagerly(appConfig.isDebug)
      .build()
      .create(ImgurUploadApi::class.java)
  }

  @SingleIn(AppScope::class)
  @Provides
  internal fun provideGithubIssueService(
    client: Lazy<OkHttpClient>,
    moshi: Moshi,
    appConfig: AppConfig,
  ): GitHubIssueApi {
    return Retrofit.Builder()
      .baseUrl("https://api.github.com/")
      .delegatingCallFactory(client)
      .addConverterFactory(MoshiConverterFactory.create(moshi))
      .validateEagerly(appConfig.isDebug)
      .build()
      .create(GitHubIssueApi::class.java)
  }
}
