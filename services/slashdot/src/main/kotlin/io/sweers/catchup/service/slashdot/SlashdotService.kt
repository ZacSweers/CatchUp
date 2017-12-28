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

package io.sweers.catchup.service.slashdot

import com.tickaroo.tikxml.TikXml
import com.tickaroo.tikxml.retrofit.TikXmlConverterFactory
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
import io.sweers.catchup.service.api.LinkHandler
import io.sweers.catchup.service.api.Service
import io.sweers.catchup.service.api.ServiceKey
import io.sweers.catchup.service.api.ServiceMeta
import io.sweers.catchup.service.api.ServiceMetaKey
import io.sweers.catchup.service.api.SummarizationInfo
import io.sweers.catchup.service.api.SummarizationType.NONE
import io.sweers.catchup.service.api.TextService
import okhttp3.OkHttpClient
import org.threeten.bp.Instant
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import javax.inject.Inject
import javax.inject.Qualifier

@Qualifier
private annotation class InternalApi

private const val SERVICE_KEY = "sd"

internal class SlashdotService @Inject constructor(
    @InternalApi private val serviceMeta: ServiceMeta,
    private val service: SlashdotApi,
    private val linkHandler: LinkHandler)
  : TextService {

  override fun meta() = serviceMeta

  override fun fetchPage(request: DataRequest): Maybe<DataResult> {
    return service.main()
        .map { it.itemList }
        .flattenAsObservable { it }
        .map { (title, id, _, summary, updated, section, comments, author, department) ->
          CatchUpItem(
              id = id.hashCode().toLong(),
              title = title,
              score = null,
              timestamp = updated,
              author = author.name,
              source = department,
              commentCount = comments,
              tag = section,
              itemClickUrl = id,
              itemCommentClickUrl = "$id#comments",
              summarizationInfo = SummarizationInfo(summary.substringBefore("<p>"), NONE)
          )
        }
        .toList()
        .toMaybe()
        .map { DataResult(it, null) }
  }

  override fun linkHandler() = linkHandler
}

@Module
abstract class SlashdotMetaModule {

  @IntoMap
  @ServiceMetaKey(SERVICE_KEY)
  @Binds
  internal abstract fun slashdotServiceMeta(@InternalApi meta: ServiceMeta): ServiceMeta

  @Module
  companion object {

    @Provides
    @JvmStatic
    @Reusable
    @InternalApi
    internal fun provideSlashdotServiceMeta() = ServiceMeta(
        SERVICE_KEY,
        R.string.slashdot,
        R.color.slashdotAccent,
        R.drawable.logo_sd,
        firstPageKey = "main"
    )
  }
}

@Module(includes = [SlashdotMetaModule::class])
abstract class SlashdotModule {

  @IntoMap
  @ServiceKey(SERVICE_KEY)
  @Binds
  internal abstract fun slashdotService(slashdotService: SlashdotService): Service

  @Module
  companion object {

    @Provides
    @JvmStatic
    internal fun provideTikXml(): TikXml = TikXml.Builder()
        .exceptionOnUnreadXml(false)
        .addTypeConverter(Instant::class.java, InstantTypeConverter())
        .build()

    @Provides
    @InternalApi
    @JvmStatic
    internal fun provideSlashdotOkHttpClient(okHttpClient: OkHttpClient): OkHttpClient {
      return okHttpClient.newBuilder()
          .addNetworkInterceptor { chain ->
            val originalResponse = chain.proceed(chain.request())
            // read from cache for 30 minutes, per slashdot's preferred limit
            val maxAge = 60 * 30
            originalResponse.newBuilder()
                .header("Cache-Control", "public, max-age=" + maxAge)
                .build()
          }
          .build()
    }

    @Provides
    @JvmStatic
    internal fun provideSlashdotApi(@InternalApi client: Lazy<OkHttpClient>,
        rxJavaCallAdapterFactory: RxJava2CallAdapterFactory,
        tikXml: TikXml): SlashdotApi {
      val retrofit = Retrofit.Builder().baseUrl(SlashdotApi.ENDPOINT)
          .callFactory { client.get().newCall(it) }
          .addCallAdapterFactory(rxJavaCallAdapterFactory)
          .addConverterFactory(TikXmlConverterFactory.create(tikXml))
          .validateEagerly(BuildConfig.DEBUG)
          .build()
      return retrofit.create(SlashdotApi::class.java)
    }
  }
}
