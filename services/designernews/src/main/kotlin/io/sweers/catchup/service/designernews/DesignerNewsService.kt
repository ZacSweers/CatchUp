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
package io.sweers.catchup.service.designernews

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
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.sweers.catchup.libraries.retrofitconverters.delegatingCallFactory
import io.sweers.catchup.service.api.CatchUpItem
import io.sweers.catchup.service.api.DataRequest
import io.sweers.catchup.service.api.DataResult
import io.sweers.catchup.service.api.Mark.Companion.createCommentMark
import io.sweers.catchup.service.api.Service
import io.sweers.catchup.service.api.ServiceKey
import io.sweers.catchup.service.api.ServiceMeta
import io.sweers.catchup.service.api.ServiceMetaKey
import io.sweers.catchup.service.api.TextService
import io.sweers.catchup.util.data.adapters.ISO8601InstantAdapter
import javax.inject.Inject
import javax.inject.Qualifier
import kotlinx.datetime.Instant
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory
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

  override fun fetchPage(request: DataRequest): Single<DataResult> {
    val page = request.pageId.toInt()
    return api
      .getTopStories(page)
      .flatMapObservable { stories -> Observable.fromIterable(stories) }
      .map { story ->
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
              )
          )
        }
      }
      .toList()
      .map { DataResult(it, (page + 1).toString()) }
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
    @Reusable
    internal fun provideDesignerNewsMeta(): ServiceMeta =
      ServiceMeta(
        SERVICE_KEY,
        R.string.dn,
        R.color.dnAccent,
        R.drawable.logo_dn,
        pagesAreNumeric = true,
        firstPageKey = "1"
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
  internal fun provideDesignerNewsService(
    client: Lazy<OkHttpClient>,
    @InternalApi moshi: Moshi,
    rxJavaCallAdapterFactory: RxJava3CallAdapterFactory,
    appConfig: AppConfig
  ): DesignerNewsApi {

    val retrofit =
      Retrofit.Builder()
        .baseUrl(DesignerNewsApi.ENDPOINT)
        .delegatingCallFactory(client)
        .addCallAdapterFactory(rxJavaCallAdapterFactory)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .validateEagerly(appConfig.isDebug)
        .build()
    return retrofit.create(DesignerNewsApi::class.java)
  }
}
