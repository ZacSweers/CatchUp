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
package catchup.service.slashdot

import catchup.appconfig.AppConfig
import catchup.di.AppScope
import catchup.libraries.retrofitconverters.delegatingCallFactory
import catchup.service.api.CatchUpItem
import catchup.service.api.ContentType
import catchup.service.api.DataRequest
import catchup.service.api.DataResult
import catchup.service.api.Mark.Companion.createCommentMark
import catchup.service.api.Service
import catchup.service.api.ServiceKey
import catchup.service.api.ServiceMeta
import catchup.service.api.ServiceMetaKey
import catchup.service.api.TextService
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.squareup.anvil.annotations.ContributesMultibinding
import com.squareup.anvil.annotations.ContributesTo
import dagger.Binds
import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import javax.inject.Inject
import javax.inject.Qualifier
import nl.adaptivity.xmlutil.serialization.XML
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit

@Qualifier private annotation class InternalApi

private const val SERVICE_KEY = "sd"

@ServiceKey(SERVICE_KEY)
@ContributesMultibinding(AppScope::class, boundType = Service::class)
class SlashdotService
@Inject
constructor(@InternalApi private val serviceMeta: ServiceMeta, private val service: SlashdotApi) :
  TextService {

  override fun meta() = serviceMeta

  override suspend fun fetch(request: DataRequest): DataResult {
    return service
      .main()
      .entries
      .mapIndexed { index, (title, id, _, _, updated, section, comments, author, department) ->
        CatchUpItem(
          id = id.hashCode().toLong(),
          title = title,
          score = null,
          timestamp = updated,
          author = author.name,
          source = department,
          tag = section,
          itemClickUrl = id,
          mark = createCommentMark(count = comments, clickUrl = "$id#comments"),
          indexInResponse = index + request.pageOffset,
          serviceId = meta().id,
          contentType = ContentType.HTML,
        )
      }
      .let { DataResult(it, null) }
  }
}

@ContributesTo(AppScope::class)
@Module
abstract class SlashdotMetaModule {

  @IntoMap
  @ServiceMetaKey(SERVICE_KEY)
  @Binds
  internal abstract fun slashdotServiceMeta(@InternalApi meta: ServiceMeta): ServiceMeta

  companion object {

    @Provides
    @InternalApi
    fun provideSlashdotServiceMeta(): ServiceMeta =
      ServiceMeta(
        SERVICE_KEY,
        R.string.catchup_service_sd_name,
        R.color.catchup_service_sd_accent,
        R.drawable.catchup_service_sd_logo,
        firstPageKey = null,
      )
  }
}

@ContributesTo(AppScope::class)
@Module(includes = [SlashdotMetaModule::class])
object SlashdotModule {

  @Provides fun provideXml(): XML = XML { defaultPolicy { ignoreUnknownChildren() } }

  @Provides
  @InternalApi
  fun provideSlashdotOkHttpClient(okHttpClient: OkHttpClient): OkHttpClient {
    return okHttpClient
      .newBuilder()
      .addNetworkInterceptor { chain ->
        val originalResponse = chain.proceed(chain.request())
        // read from cache for 30 minutes, per slashdot's preferred limit
        val maxAge = 60 * 30
        originalResponse.newBuilder().header("Cache-Control", "public, max-age=$maxAge").build()
      }
      .build()
  }

  @Provides
  fun provideSlashdotApi(
    @InternalApi client: Lazy<OkHttpClient>,
    xml: XML,
    appConfig: AppConfig,
  ): SlashdotApi {
    val contentType = "application/xml".toMediaType()
    val retrofit =
      Retrofit.Builder()
        .baseUrl(SlashdotApi.ENDPOINT)
        .delegatingCallFactory(client)
        .addConverterFactory(xml.asConverterFactory(contentType))
        .validateEagerly(appConfig.isDebug)
        .build()
    return retrofit.create(SlashdotApi::class.java)
  }
}
