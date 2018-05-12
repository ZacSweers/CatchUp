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

package io.sweers.catchup.service.newsapi

import com.squareup.moshi.Moshi
import dagger.Binds
import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.Reusable
import dagger.multibindings.IntoMap
import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.SingleSource
import io.sweers.catchup.service.api.CatchUpItem
import io.sweers.catchup.service.api.DataRequest
import io.sweers.catchup.service.api.DataResult
import io.sweers.catchup.service.api.LinkHandler
import io.sweers.catchup.service.api.Service
import io.sweers.catchup.service.api.ServiceKey
import io.sweers.catchup.service.api.ServiceMeta
import io.sweers.catchup.service.api.ServiceMetaKey
import io.sweers.catchup.service.api.SummarizationInfo
import io.sweers.catchup.service.api.TextService
import io.sweers.catchup.service.newsapi.Language.EN
import io.sweers.catchup.service.newsapi.SortBy.POPULARITY
import io.sweers.catchup.service.newsapi.model.NewsApiResponse
import io.sweers.catchup.service.newsapi.model.NewsApiResponseFactory
import io.sweers.catchup.service.newsapi.model.Success
import io.sweers.catchup.serviceregistry.annotations.Meta
import io.sweers.catchup.serviceregistry.annotations.ServiceModule
import io.sweers.catchup.util.data.adapters.ISO8601InstantAdapter
import io.sweers.catchup.util.network.AuthInterceptor
import io.sweers.catchup.util.rx.filterIsInstance
import okhttp3.OkHttpClient
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.temporal.ChronoUnit
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Inject
import javax.inject.Qualifier

@Qualifier
private annotation class InternalApi

private const val SERVICE_KEY = "ns"

internal class NewsApiService @Inject constructor(
    @InternalApi private val serviceMeta: ServiceMeta,
    @InternalApi private val errorConverter: ErrorConverter,
    private val api: NewsApiApi,
    private val linkHandler: LinkHandler)
  : TextService {

  override fun meta() = serviceMeta

  override fun fetchPage(request: DataRequest): Maybe<DataResult> {
    val twelveHoursAgoISO = DateTimeFormatter.ISO_LOCAL_DATE
        .withZone(ZoneId.systemDefault())
        .format(Instant.now().minus(12, ChronoUnit.HOURS))
    val page = request.pageId.toInt()
    return api
        .getStories(pageSize = 50,
            page = page,
            from = twelveHoursAgoISO,
            query = "tech", // Required but not sure how to get results that aren't garbage
            language = EN,
            sortBy = POPULARITY
        )
        .onErrorResumeNext(errorConverter)
        .filterIsInstance<Success>()
        .flattenAsObservable(Success::articles)
        .map {
          with(it) {
            CatchUpItem(
                id = url.hashCode().toLong(),
                title = title,
                timestamp = publishedAt,
                source = source.name,
                author = author,
                itemClickUrl = url,
                summarizationInfo = SummarizationInfo.from(it.url)
            )
          }
        }
        .toList()
        .map { DataResult(it, (page + 1).toString()) }
        .toMaybe()
  }

  override fun linkHandler() = linkHandler
}

@Meta
@ServiceModule
@Module
abstract class NewsApiMetaModule {

  @IntoMap
  @ServiceMetaKey(SERVICE_KEY)
  @Binds
  internal abstract fun newsApiServiceMeta(@InternalApi meta: ServiceMeta): ServiceMeta

  @Module
  companion object {

    @InternalApi
    @Provides
    @Reusable
    @JvmStatic
    internal fun provideNewsApiServiceMeta() = ServiceMeta(
        SERVICE_KEY,
        R.string.ns,
        R.color.nsAccent,
        R.drawable.logo_ns,
        pagesAreNumeric = true,
        firstPageKey = "1"
    )
  }
}

@ServiceModule
@Module(includes = [NewsApiMetaModule::class])
abstract class NewsApiModule {

  @IntoMap
  @ServiceKey(SERVICE_KEY)
  @Binds
  internal abstract fun newsApiService(newsApiService: NewsApiService): Service

  @Module
  companion object {

    @Provides
    @InternalApi
    @JvmStatic
    internal fun provideNewsApiOkHttpClient(
        client: OkHttpClient): OkHttpClient {
      return client.newBuilder()
          .addInterceptor(AuthInterceptor("Bearer", BuildConfig.NEWS_API_API_KEY))
          .build()
    }

    @Provides
    @InternalApi
    @JvmStatic
    internal fun provideNewsApiMoshi(moshi: Moshi): Moshi {
      return moshi.newBuilder()
          .add(NewsApiResponseFactory())
          .add(Instant::class.java, ISO8601InstantAdapter())
          .build()
    }

    @Provides
    @JvmStatic
    internal fun provideErrorConverter(moshi: Moshi): ErrorConverter {
      return object : ErrorConverter {
        override fun apply(t: Throwable): SingleSource<NewsApiResponse> {
          return if (t is HttpException) {
            Single.just(moshi.adapter(NewsApiResponse::class.java)
                .fromJson(t.response().errorBody()!!.source()))
          } else {
            Single.error(t)
          }
        }
      }
    }

    @Provides
    @JvmStatic
    internal fun provideNewsApiService(
        @InternalApi client: Lazy<OkHttpClient>,
        @InternalApi moshi: Moshi,
        rxJavaCallAdapterFactory: RxJava2CallAdapterFactory): NewsApiApi {
      return Retrofit.Builder().baseUrl(NewsApiApi.ENDPOINT)
          .callFactory(client.get()::newCall)
          .addCallAdapterFactory(rxJavaCallAdapterFactory)
          .addConverterFactory(MoshiConverterFactory.create(moshi))
          .validateEagerly(BuildConfig.DEBUG)
          .build()
          .create(NewsApiApi::class.java)
    }
  }
}
