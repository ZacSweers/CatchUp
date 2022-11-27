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
package io.sweers.catchup.service.unsplash

import com.squareup.anvil.annotations.ContributesMultibinding
import com.squareup.anvil.annotations.ContributesTo
import com.squareup.moshi.Moshi
import dagger.Binds
import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.Reusable
import dagger.multibindings.IntoMap
import dev.zacsweers.catchup.appconfig.AppConfig
import dev.zacsweers.catchup.di.AppScope
import io.reactivex.rxjava3.core.Single
import io.sweers.catchup.libraries.retrofitconverters.delegatingCallFactory
import io.sweers.catchup.service.api.CatchUpItem
import io.sweers.catchup.service.api.DataRequest
import io.sweers.catchup.service.api.DataResult
import io.sweers.catchup.service.api.ImageInfo
import io.sweers.catchup.service.api.Service
import io.sweers.catchup.service.api.ServiceKey
import io.sweers.catchup.service.api.ServiceMeta
import io.sweers.catchup.service.api.ServiceMetaKey
import io.sweers.catchup.service.api.VisualService
import io.sweers.catchup.service.api.VisualService.SpanConfig
import io.sweers.catchup.util.data.adapters.ISO8601InstantAdapter
import io.sweers.catchup.util.network.AuthInterceptor
import javax.inject.Inject
import javax.inject.Qualifier
import kotlinx.datetime.Instant
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory
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

  override fun fetchPage(request: DataRequest): Single<DataResult> {
    val page = request.pageId.toInt()
    return api
      .getPhotos(page, 50)
      .flattenAsObservable { it }
      .map {
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
              imageId = it.id
            )
        )
      }
      .toList()
      .map { DataResult(it, (page + 1).toString()) }
  }

  override fun spanConfig() =
    SpanConfig(3) {
      /* emulating https://material-design.storage.googleapis.com/publish/material_v_4/material_ext_publish/0B6Okdz75tqQsck9lUkgxNVZza1U/style_imagery_integration_scale1.png */
      when (it % 6) {
        5 -> 3
        3 -> 2
        else -> 1
      }
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
    @Reusable
    internal fun provideUnsplashServiceMeta(): ServiceMeta =
      ServiceMeta(
        SERVICE_KEY,
        R.string.unsplash,
        R.color.unsplashAccent,
        R.drawable.logo_unsplash,
        isVisual = true,
        pagesAreNumeric = true,
        firstPageKey = "1",
        enabled = BuildConfig.UNSPLASH_API_KEY.run { !isNullOrEmpty() && !equals("null") }
      )
  }
}

@ContributesTo(AppScope::class)
@Module(includes = [UnsplashMetaModule::class])
object UnsplashModule {

  @Provides
  @InternalApi
  internal fun provideUnsplashMoshi(moshi: Moshi): Moshi {
    return moshi.newBuilder().add(Instant::class.java, ISO8601InstantAdapter()).build()
  }

  @Provides
  @InternalApi
  internal fun provideUnsplashOkHttpClient(client: OkHttpClient): OkHttpClient {
    return client
      .newBuilder()
      .addInterceptor {
        it.proceed(it.request().newBuilder().addHeader("Accept-Version", "v1").build())
      }
      .addInterceptor(AuthInterceptor("Client-ID", BuildConfig.UNSPLASH_API_KEY))
      .build()
  }

  @Provides
  internal fun provideUnsplashService(
    @InternalApi client: Lazy<OkHttpClient>,
    @InternalApi moshi: Moshi,
    rxJavaCallAdapterFactory: RxJava3CallAdapterFactory,
    appConfig: AppConfig
  ): UnsplashApi {
    return Retrofit.Builder()
      .baseUrl(UnsplashApi.ENDPOINT)
      .delegatingCallFactory(client)
      .addCallAdapterFactory(rxJavaCallAdapterFactory)
      .addConverterFactory(MoshiConverterFactory.create(moshi))
      .validateEagerly(appConfig.isDebug)
      .build()
      .create(UnsplashApi::class.java)
  }
}
