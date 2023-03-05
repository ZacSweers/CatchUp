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
package io.sweers.catchup.service.uplabs

import com.squareup.anvil.annotations.ContributesMultibinding
import com.squareup.anvil.annotations.ContributesTo
import com.squareup.moshi.Moshi
import dagger.Binds
import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import dev.zacsweers.catchup.appconfig.AppConfig
import dev.zacsweers.catchup.di.AppScope
import io.sweers.catchup.libraries.retrofitconverters.delegatingCallFactory
import io.sweers.catchup.service.api.CatchUpItem
import io.sweers.catchup.service.api.ContentType
import io.sweers.catchup.service.api.DataRequest
import io.sweers.catchup.service.api.DataResult
import io.sweers.catchup.service.api.ImageInfo
import io.sweers.catchup.service.api.Service
import io.sweers.catchup.service.api.ServiceKey
import io.sweers.catchup.service.api.ServiceMeta
import io.sweers.catchup.service.api.ServiceMetaKey
import io.sweers.catchup.service.api.VisualService
import io.sweers.catchup.util.data.adapters.ISO8601InstantAdapter
import javax.inject.Inject
import javax.inject.Qualifier
import kotlinx.datetime.Instant
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory

@Qualifier private annotation class InternalApi

private const val SERVICE_KEY = "uplabs"

@ServiceKey(SERVICE_KEY)
@ContributesMultibinding(AppScope::class, boundType = Service::class)
class UplabsService
@Inject
constructor(@InternalApi private val serviceMeta: ServiceMeta, private val api: UplabsApi) :
  VisualService {

  override fun meta() = serviceMeta

  override suspend fun fetch(request: DataRequest): DataResult {
    val page = request.pageKey!!.toInt()
    return api
      .getPopular(page, 1)
      .mapIndexed { index, it ->
        CatchUpItem(
          id = it.id.hashCode().toLong(),
          title = it.name,
          score = "▲" to it.points,
          timestamp = it.showcasedAt,
          author = it.makerName,
          source = it.label,
          tag = it.category,
          itemClickUrl = if (it.animated) it.animatedTeaserUrl else it.teaserUrl,
          imageInfo =
            ImageInfo(
              url = if (it.animated) it.animatedTeaserUrl else it.teaserUrl,
              detailUrl = it.previewUrl, // Both animated and not are the preview url
              animatable = it.animated,
              sourceUrl = it.url,
              bestSize = null,
              imageId = it.id.toString()
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
abstract class UplabsMetaModule {

  @IntoMap
  @ServiceMetaKey(SERVICE_KEY)
  @Binds
  internal abstract fun uplabsServiceMeta(@InternalApi meta: ServiceMeta): ServiceMeta

  companion object {

    @InternalApi
    @Provides
    internal fun provideUplabsServiceMeta(): ServiceMeta =
      ServiceMeta(
        SERVICE_KEY,
        R.string.uplabs,
        R.color.uplabsAccent,
        R.drawable.logo_uplabs,
        isVisual = true,
        pagesAreNumeric = true,
        firstPageKey = 0
      )
  }
}

@ContributesTo(AppScope::class)
@Module(includes = [UplabsMetaModule::class])
object UplabsModule {
  @Provides
  @InternalApi
  internal fun provideUplabsMoshi(moshi: Moshi): Moshi {
    return moshi.newBuilder().add(Instant::class.java, ISO8601InstantAdapter()).build()
  }

  @Provides
  internal fun provideUplabsService(
    client: Lazy<OkHttpClient>,
    @InternalApi moshi: Moshi,
    rxJavaCallAdapterFactory: RxJava3CallAdapterFactory,
    appConfig: AppConfig
  ): UplabsApi {
    return Retrofit.Builder()
      .baseUrl(UplabsApi.ENDPOINT)
      .delegatingCallFactory(client)
      .addCallAdapterFactory(rxJavaCallAdapterFactory)
      .addConverterFactory(MoshiConverterFactory.create(moshi))
      .validateEagerly(appConfig.isDebug)
      .build()
      .create(UplabsApi::class.java)
  }
}
