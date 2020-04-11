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
    val retrofit = Retrofit.Builder().baseUrl("https://api.linkpreview.net")
        .delegatingCallFactory(client)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .validateEagerly(appConfig.isDebug)
        .build()
    return retrofit.create(UrlPreview::class.java)
  }
}