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

package io.sweers.catchup.service.imgur

import com.serjltt.moshi.adapters.Wrapped
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
import io.sweers.catchup.util.data.adapters.EpochInstantJsonAdapter
import io.sweers.catchup.util.network.AuthInterceptor
import okhttp3.OkHttpClient
import org.threeten.bp.Instant
import retrofit2.Retrofit.Builder
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Inject
import javax.inject.Qualifier

@Qualifier
private annotation class InternalApi

private const val SERVICE_KEY = "imgur"

internal class ImgurService @Inject constructor(
    @InternalApi private val serviceMeta: ServiceMeta,
    private val api: ImgurApi,
    private val linkHandler: LinkHandler)
  : VisualService {

  override fun meta() = serviceMeta

  override fun fetchPage(request: DataRequest): Maybe<DataResult> {
    val page = request.pageId.toInt()
    return api.subreddit("EarthPorn", page)
        .flattenAsObservable { it }
        .map {
          val resolvedLink = it.resolveDisplayLink()
          CatchUpItem(
              id = it.id().hashCode().toLong(),
              title = it.title(),
              score = "⬆" to it.resolveScore(),
              timestamp = it.datetime(),
              author = it.accountUrl(),
              itemClickUrl = it.resolveClickLink(),
              imageInfo = ImageInfo(
                  resolvedLink,
                  resolvedLink.endsWith(".gif"),
                  null
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
abstract class ImgurMetaModule {

  @IntoMap
  @ServiceMetaKey(SERVICE_KEY)
  @Binds
  internal abstract fun imgurServiceMeta(@InternalApi meta: ServiceMeta): ServiceMeta

  @Module
  companion object {

    @InternalApi
    @Provides
    @Reusable
    @JvmStatic
    internal fun provideImgurServiceMeta() = ServiceMeta(
        SERVICE_KEY,
        R.string.imgur,
        R.color.imgurAccent,
        R.drawable.logo_imgur,
        isVisual = true,
        pagesAreNumeric = true,
        firstPageKey = "0"
    )
  }
}

@Module(includes = [ImgurMetaModule::class])
abstract class ImgurModule {

  @IntoMap
  @ServiceKey(SERVICE_KEY)
  @Binds
  internal abstract fun imgurService(imgurService: ImgurService): Service

  @Module
  companion object {

    @Provides
    @InternalApi
    @JvmStatic
    internal fun provideImgurOkHttpClient(
        client: OkHttpClient): OkHttpClient {
      return client.newBuilder()
          .addInterceptor(AuthInterceptor("Client-ID",
              BuildConfig.IMGUR_CLIENT_ACCESS_TOKEN))
          .build()
    }

    @Provides
    @InternalApi
    @JvmStatic
    internal fun provideImgurMoshi(moshi: Moshi): Moshi {
      return moshi.newBuilder()
          .add(ImgurAdapterFactory.create())
          .add(Wrapped.ADAPTER_FACTORY)
          .add(Instant::class.java, EpochInstantJsonAdapter())
          .build()
    }

    @Provides
    @JvmStatic
    internal fun provideImgurService(@InternalApi client: Lazy<OkHttpClient>,
        @InternalApi moshi: Moshi,
        rxJavaCallAdapterFactory: RxJava2CallAdapterFactory): ImgurApi {
      return Builder().baseUrl(ImgurApi.Companion.ENDPOINT)
          .callFactory { client.get().newCall(it) }
          .addCallAdapterFactory(rxJavaCallAdapterFactory)
          .addConverterFactory(MoshiConverterFactory.create(moshi))
          .validateEagerly(BuildConfig.DEBUG)
          .build()
          .create(ImgurApi::class.java)
    }
  }
}
