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

import com.squareup.moshi.Moshi
import dagger.Binds
import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.Reusable
import dagger.multibindings.IntoMap
import io.reactivex.Observable
import io.reactivex.Single
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
import io.sweers.catchup.serviceregistry.annotations.Meta
import io.sweers.catchup.serviceregistry.annotations.ServiceModule
import io.sweers.catchup.util.data.adapters.ISO8601InstantAdapter
import okhttp3.OkHttpClient
import org.threeten.bp.Instant
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Inject
import javax.inject.Qualifier

@Qualifier
private annotation class InternalApi

private const val SERVICE_KEY = "dn"

internal class DesignerNewsService @Inject constructor(
  @InternalApi private val serviceMeta: ServiceMeta,
  private val api: DesignerNewsApi
) :
  TextService {

  override fun meta() = serviceMeta

  override fun fetchPage(request: DataRequest): Single<DataResult> {
    val page = request.pageId.toInt()
    return api.getTopStories(page)
        .flatMapObservable { stories ->
          Observable.fromIterable(stories)
        }
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
                mark = createCommentMark(
                    count = commentCount,
                    clickUrl = href.replace("api.", "www.")
                        .replace("api/v2/", "")
                )
            )
          }
        }
        .toList()
        .map { DataResult(it, (page + 1).toString()) }
  }
}

@Meta
@ServiceModule
@Module
abstract class DesignerNewsMetaModule {

  @IntoMap
  @ServiceMetaKey(SERVICE_KEY)
  @Binds
  internal abstract fun designerNewsServiceMeta(@InternalApi meta: ServiceMeta): ServiceMeta

  @Module
  companion object {

    @InternalApi
    @Provides
    @Reusable
    @JvmStatic
    internal fun provideDesignerNewsMeta() = ServiceMeta(
        SERVICE_KEY,
        R.string.dn,
        R.color.dnAccent,
        R.drawable.logo_dn,
        pagesAreNumeric = true,
        firstPageKey = "1"
    )
  }
}

@ServiceModule
@Module(includes = [DesignerNewsMetaModule::class])
abstract class DesignerNewsModule {

  @IntoMap
  @ServiceKey(SERVICE_KEY)
  @Binds
  internal abstract fun designerNewsService(service: DesignerNewsService): Service

  @Module
  companion object {

    @Provides
    @InternalApi
    @JvmStatic
    internal fun provideDesignerNewsMoshi(moshi: Moshi): Moshi {
      return moshi.newBuilder()
          .add(Instant::class.java, ISO8601InstantAdapter())
          .build()
    }

    @Provides
    @JvmStatic
    internal fun provideDesignerNewsService(
      client: Lazy<OkHttpClient>,
      @InternalApi moshi: Moshi,
      rxJavaCallAdapterFactory: RxJava2CallAdapterFactory
    ): DesignerNewsApi {

      val retrofit = Retrofit.Builder().baseUrl(
          DesignerNewsApi.ENDPOINT)
          .delegatingCallFactory(client)
          .addCallAdapterFactory(rxJavaCallAdapterFactory)
          .addConverterFactory(MoshiConverterFactory.create(moshi))
          // .validateEagerly(BuildConfig.DEBUG) // Enable with cross-module debug build configs
          .build()
      return retrofit.create(DesignerNewsApi::class.java)
    }
  }
}
