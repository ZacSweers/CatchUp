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
package catchup.service.designernews

import com.squareup.anvil.annotations.ContributesMultibinding
import com.squareup.anvil.annotations.ContributesTo
import com.squareup.moshi.Moshi
import dagger.Binds
import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
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
import catchup.util.data.adapters.ISO8601InstantAdapter
import javax.inject.Inject
import javax.inject.Qualifier
import kotlinx.datetime.Instant
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

@Qualifier private annotation class InternalApi

private const val SERVICE_KEY = "dn"

@ServiceKey(SERVICE_KEY)
@ContributesMultibinding(AppScope::class, boundType = Service::class)
class DesignerNewsService
@Inject
constructor(@InternalApi private val serviceMeta: ServiceMeta, private val api: DesignerNewsApi) :
  TextService {

  override fun meta() = serviceMeta

  override suspend fun fetch(request: DataRequest): DataResult {
    val page = request.pageKey!!.toInt()
    return api
      .getTopStories(page)
      .mapIndexed { index, story ->
        with(story) {
          CatchUpItem(
            id = id.toLong(),
            title = title,
            score = "▲" to voteCount,
            timestamp = createdAt,
            source = hostname,
            tag = badge,
            itemClickUrl = url,
            mark =
              createCommentMark(
                count = commentCount,
                clickUrl = href.replace("api.", "www.").replace("api/v2/", "")
              ),
            indexInResponse = index + request.pageOffset,
            serviceId = meta().id,
            contentType = ContentType.HTML,
          )
        }
      }
      .let { items -> DataResult(items, (page + 1).toString().takeUnless { items.isEmpty() }) }
  }
}

@ContributesTo(AppScope::class)
@Module
abstract class DesignerNewsMetaModule {

  @IntoMap
  @ServiceMetaKey(SERVICE_KEY)
  @Binds
  internal abstract fun designerNewsServiceMeta(@InternalApi meta: ServiceMeta): ServiceMeta

  companion object {

    @InternalApi
    @Provides
    internal fun provideDesignerNewsMeta(): ServiceMeta =
      ServiceMeta(
        SERVICE_KEY,
        R.string.catchup_service_dn_name,
        R.color.catchup_service_dn_accent,
        R.drawable.catchup_service_dn_logo,
        pagesAreNumeric = true,
        firstPageKey = 1
      )
  }
}

@ContributesTo(AppScope::class)
@Module(includes = [DesignerNewsMetaModule::class])
object DesignerNewsModule {
  @Provides
  @InternalApi
  internal fun provideDesignerNewsMoshi(moshi: Moshi): Moshi {
    return moshi.newBuilder().add(Instant::class.java, ISO8601InstantAdapter()).build()
  }

  @Provides
  internal fun provideDesignerNewsApi(
    client: Lazy<OkHttpClient>,
    @InternalApi moshi: Moshi,
    appConfig: AppConfig
  ): DesignerNewsApi {

    val retrofit =
      Retrofit.Builder()
        .baseUrl(DesignerNewsApi.ENDPOINT)
        .delegatingCallFactory(client)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .validateEagerly(appConfig.isDebug)
        .build()
    return retrofit.create(DesignerNewsApi::class.java)
  }
}
