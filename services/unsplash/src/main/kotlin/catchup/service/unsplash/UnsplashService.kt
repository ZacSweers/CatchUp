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
package catchup.service.unsplash

import catchup.appconfig.AppConfig
import catchup.di.AppScope
import catchup.libraries.retrofitconverters.delegatingCallFactory
import catchup.service.api.CatchUpItem
import catchup.service.api.ContentType
import catchup.service.api.DataRequest
import catchup.service.api.DataResult
import catchup.service.api.ImageInfo
import catchup.service.api.Service
import catchup.service.api.ServiceKey
import catchup.service.api.ServiceMeta
import catchup.service.api.ServiceMetaKey
import catchup.service.api.VisualService
import catchup.util.data.adapters.ISO8601InstantAdapter
import catchup.util.network.AuthInterceptor
import com.squareup.anvil.annotations.ContributesMultibinding
import com.squareup.anvil.annotations.ContributesTo
import com.squareup.moshi.Moshi
import dagger.Binds
import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import javax.inject.Inject
import javax.inject.Qualifier
import kotlinx.datetime.Instant
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

@Qualifier private annotation class InternalApi

private const val SERVICE_KEY = "unsplash"

@ServiceKey(SERVICE_KEY)
@ContributesMultibinding(AppScope::class, boundType = Service::class)
class UnsplashService
@Inject
constructor(@InternalApi private val serviceMeta: ServiceMeta, private val api: UnsplashApi) :
  VisualService {

  override fun meta() = serviceMeta

  override suspend fun fetch(request: DataRequest): DataResult {
    val page = request.pageKey!!.toInt()
    return api
      .getPhotos(page, request.limit)
      .mapIndexed { index, it ->
        CatchUpItem(
          id = it.id.hashCode().toLong(),
          title = "",
          score =
            "\u2665\uFE0E" // Because lol: https://code.google.com/p/android/issues/detail?id=231068
            to it.likes,
          timestamp = it.createdAt,
          author = it.user.name,
          source = null,
          tag = null,
          itemClickUrl = it.urls.full,
          imageInfo =
            ImageInfo(
              url = it.urls.small,
              detailUrl = it.urls.raw,
              animatable = false,
              sourceUrl = it.links.html,
              bestSize = null,
              aspectRatio = it.width.toFloat() / it.height.toFloat(),
              imageId = it.id,
              blurHash = it.blurHash,
              color = it.color,
            ),
          indexInResponse = index + request.pageOffset,
          serviceId = meta().id,
          contentType = ContentType.IMAGE,
        )
      }
      .let { DataResult(it, (page + 1).toString()) }
  }
}

@ContributesTo(AppScope::class)
@Module
abstract class UnsplashMetaModule {

  @IntoMap
  @ServiceMetaKey(SERVICE_KEY)
  @Binds
  internal abstract fun unsplashServiceMeta(@InternalApi meta: ServiceMeta): ServiceMeta

  companion object {

    @InternalApi
    @Provides
    fun provideUnsplashServiceMeta(): ServiceMeta =
      ServiceMeta(
        SERVICE_KEY,
        R.string.catchup_service_unsplash_name,
        R.color.catchup_service_unsplash_accent,
        R.drawable.catchup_service_unsplash_logo,
        isVisual = true,
        pagesAreNumeric = true,
        firstPageKey = 1,
        enabled = BuildConfig.UNSPLASH_API_KEY.run { !isNullOrEmpty() && !equals("null") },
      )
  }
}

@ContributesTo(AppScope::class)
@Module(includes = [UnsplashMetaModule::class])
object UnsplashModule {

  @Provides
  @InternalApi
  fun provideUnsplashMoshi(moshi: Moshi): Moshi {
    return moshi.newBuilder().add(Instant::class.java, ISO8601InstantAdapter()).build()
  }

  @Provides
  @InternalApi
  fun provideUnsplashOkHttpClient(client: OkHttpClient): OkHttpClient {
    return client
      .newBuilder()
      .addInterceptor {
        it.proceed(it.request().newBuilder().addHeader("Accept-Version", "v1").build())
      }
      .addInterceptor(AuthInterceptor("Client-ID", BuildConfig.UNSPLASH_API_KEY))
      .build()
  }

  @Provides
  fun provideUnsplashService(
    @InternalApi client: Lazy<OkHttpClient>,
    @InternalApi moshi: Moshi,
    appConfig: AppConfig,
  ): UnsplashApi {
    return Retrofit.Builder()
      .baseUrl(UnsplashApi.ENDPOINT)
      .delegatingCallFactory(client)
      .addConverterFactory(MoshiConverterFactory.create(moshi))
      .validateEagerly(appConfig.isDebug)
      .build()
      .create(UnsplashApi::class.java)
  }
}
