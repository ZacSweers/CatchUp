/*
 * Copyright (c) 2017 Zac Sweers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.sweers.catchup.service.dribbble

import com.squareup.moshi.Moshi
import dagger.Binds
import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.Reusable
import dagger.multibindings.IntoMap
import io.reactivex.Maybe
import io.sweers.catchup.service.api.CatchUpItem
import io.sweers.catchup.service.api.DataRequest
import io.sweers.catchup.service.api.DataResult
import io.sweers.catchup.service.api.ImageInfo
import io.sweers.catchup.service.api.LinkHandler
import io.sweers.catchup.service.api.Service
import io.sweers.catchup.service.api.ServiceKey
import io.sweers.catchup.service.api.ServiceMeta
import io.sweers.catchup.service.api.ServiceMetaKey
import io.sweers.catchup.service.api.VisualService
import io.sweers.catchup.util.data.adapters.ISO8601InstantAdapter
import io.sweers.catchup.util.network.AuthInterceptor
import okhttp3.OkHttpClient
import org.threeten.bp.Instant
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Inject
import javax.inject.Qualifier

@Qualifier
private annotation class InternalApi

private const val SERVICE_KEY = "dribbble"

internal class DribbbleService @Inject constructor(
    @InternalApi private val serviceMeta: ServiceMeta,
    private val api: DribbbleApi,
    private val linkHandler: LinkHandler)
  : VisualService {

  override fun meta() = serviceMeta

  override fun fetchPage(request: DataRequest): Maybe<DataResult> {
    val page = request.pageId.toInt()
    return api.getPopular(page, 50)
        .flattenAsObservable { it }
        .map {
          CatchUpItem(
              id = it.id(),
              title = "",
              score = "+" to it.likesCount().toInt(),
              timestamp = it.createdAt(),
              author = "/u/" + it.user().name(),
              source = null,
              commentCount = it.commentsCount().toInt(),
              tag = null,
              itemClickUrl = it.htmlUrl(),
              imageInfo = ImageInfo(
                  it.images().best(),
                  it.animated(),
                  it.images().bestSize()
              )
          )
        }
        .toList()
        .map { DataResult(it, (page + 1).toString()) }
        .toMaybe()
  }

  override fun linkHandler() = linkHandler
}

@Module
abstract class DribbbleMetaModule {

  @IntoMap
  @ServiceMetaKey(SERVICE_KEY)
  @Binds
  internal abstract fun dribbbleServiceMeta(@InternalApi meta: ServiceMeta): ServiceMeta

  @Module
  companion object {

    @InternalApi
    @Provides
    @Reusable
    @JvmStatic
    internal fun provideDribbbleServiceMeta() = ServiceMeta(
        SERVICE_KEY,
        R.string.dribbble,
        R.color.dribbbleAccent,
        R.drawable.logo_dribbble,
        isVisual = true,
        pagesAreNumeric = true,
        firstPageKey = "0"
    )
  }
}

@Module(includes = [DribbbleMetaModule::class])
abstract class DribbbleModule {

  @IntoMap
  @ServiceKey(SERVICE_KEY)
  @Binds
  internal abstract fun dribbbleService(dribbbleService: DribbbleService): Service

  @Module
  companion object {

    @Provides
    @InternalApi
    @JvmStatic
    internal fun provideDribbbleOkHttpClient(
        client: OkHttpClient): OkHttpClient {
      return client.newBuilder()
          .addInterceptor(AuthInterceptor("Bearer",
              BuildConfig.DRIBBBLE_CLIENT_ACCESS_TOKEN))
          .build()
    }

    @Provides
    @InternalApi
    @JvmStatic
    internal fun provideDribbbleMoshi(moshi: Moshi): Moshi {
      return moshi.newBuilder()
          .add(DribbbleAdapterFactory.create())
          .add(Instant::class.java, ISO8601InstantAdapter())
          .build()
    }

    @Provides
    @JvmStatic
    internal fun provideDribbbleService(@InternalApi client: Lazy<OkHttpClient>,
        @InternalApi moshi: Moshi,
        rxJavaCallAdapterFactory: RxJava2CallAdapterFactory): DribbbleApi {
      return Retrofit.Builder().baseUrl(DribbbleApi.ENDPOINT)
          .callFactory { client.get().newCall(it) }
          .addCallAdapterFactory(rxJavaCallAdapterFactory)
          .addConverterFactory(MoshiConverterFactory.create(moshi))
          .validateEagerly(BuildConfig.DEBUG)
          .build()
          .create(DribbbleApi::class.java)
    }
  }
}
