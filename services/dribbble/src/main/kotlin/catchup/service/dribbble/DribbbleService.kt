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
package catchup.service.dribbble

import catchup.appconfig.AppConfig
import catchup.libraries.retrofitconverters.DecodingConverter
import catchup.libraries.retrofitconverters.delegatingCallFactory
import catchup.service.api.CatchUpItem
import catchup.service.api.ContentType
import catchup.service.api.DataRequest
import catchup.service.api.DataResult
import catchup.service.api.ImageInfo
import catchup.service.api.Mark.Companion.createCommentMark
import catchup.service.api.Service
import catchup.service.api.ServiceKey
import catchup.service.api.ServiceMeta
import catchup.service.api.ServiceMetaKey
import catchup.service.api.VisualService
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.Qualifier
import dev.zacsweers.metro.binding
import okhttp3.OkHttpClient
import retrofit2.Retrofit

@Qualifier private annotation class InternalApi

private const val SERVICE_KEY = "dribbble"

@ServiceKey(SERVICE_KEY)
@ContributesIntoMap(AppScope::class, binding = binding<Service>())
@Inject
class DribbbleService(
  @InternalApi private val serviceMeta: ServiceMeta,
  private val api: DribbbleApi,
) : VisualService {

  override fun meta() = serviceMeta

  override suspend fun fetch(request: DataRequest): DataResult {
    val page = request.pageKey!!.toInt()
    return api
      .getPopular(page, request.limit)
      .mapIndexed { index, it ->
        CatchUpItem(
          id = it.id,
          title = "",
          score = "+" to it.likesCount.toInt(),
          timestamp = it.createdAt,
          author = "/u/" + it.user.name,
          source = null,
          tag = null,
          itemClickUrl = it.imageUrl,
          imageInfo =
            ImageInfo(
              url = it.imageUrl,
              detailUrl = it.imageUrl,
              animatable = false, // TODO these are videos
              sourceUrl = it.htmlUrl,
              bestSize = null,
              aspectRatio = 4 / 3f,
              imageId = it.id.toString(),
            ),
          mark = createCommentMark(count = it.commentsCount.toInt()),
          indexInResponse = index + request.pageOffset,
          serviceId = meta().id,
          contentType = ContentType.IMAGE,
        )
      }
      .let { DataResult(it, (page + 1).toString()) }
  }
}

@ContributesTo(AppScope::class)
interface DribbbleMetaModule {

  @IntoMap
  @ServiceMetaKey(SERVICE_KEY)
  @Binds
  fun dribbbleServiceMeta(@InternalApi meta: ServiceMeta): ServiceMeta

  companion object {

    @InternalApi
    @Provides
    fun provideDribbbleServiceMeta(): ServiceMeta =
      ServiceMeta(
        SERVICE_KEY,
        R.string.catchup_service_dribbble_name,
        R.color.catchup_service_dribbble_accent,
        R.drawable.catchup_service_dribbble_logo,
        isVisual = true,
        pagesAreNumeric = true,
        firstPageKey = 1,
      )
  }
}

@ContributesTo(AppScope::class)
interface DribbbleModule {
  @Provides
  fun provideDribbbleService(client: Lazy<OkHttpClient>, appConfig: AppConfig): DribbbleApi {
    return Retrofit.Builder()
      .baseUrl(DribbbleApi.ENDPOINT)
      .delegatingCallFactory(client)
      .addConverterFactory(DecodingConverter.newFactory(DribbbleParser::parse))
      .validateEagerly(appConfig.isDebug)
      .build()
      .create(DribbbleApi::class.java)
  }
}
