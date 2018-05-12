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

package io.sweers.catchup.service.medium

import com.squareup.moshi.Moshi
import dagger.Binds
import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.Reusable
import dagger.multibindings.IntoMap
import io.reactivex.Maybe
import io.reactivex.Observable
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
import io.sweers.catchup.service.medium.model.MediumPost
import io.sweers.catchup.service.medium.model.Post
import io.sweers.catchup.serviceregistry.annotations.Meta
import io.sweers.catchup.serviceregistry.annotations.ServiceModule
import io.sweers.catchup.util.data.adapters.EpochInstantJsonAdapter
import io.sweers.inspector.Inspector
import okhttp3.OkHttpClient
import org.threeten.bp.Instant
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit.MILLISECONDS
import javax.inject.Inject
import javax.inject.Qualifier

@Qualifier
private annotation class InternalApi

private const val SERVICE_KEY = "medium"

internal class MediumService @Inject constructor(
    @InternalApi private val serviceMeta: ServiceMeta,
    private val api: MediumApi,
    private val linkHandler: LinkHandler)
  : TextService {

  override fun meta() = serviceMeta

  override fun fetchPage(request: DataRequest): Maybe<DataResult> {
    return api.top()
        .concatMapEager { references ->
          Observable.fromIterable<Post>(references.post.values)
              .map { post ->
                MediumPost(
                    post = post,
                    user = references.user[post.creatorId]
                        ?: throw IllegalStateException("Missing user on post!"),
                    collection = references.collection[post.homeCollectionId])
              }
        }
        .map {
          with(it) {
            val url = constructUrl()
            CatchUpItem(
                id = post.id.hashCode().toLong(),
                title = post.title,
                score =
                "\u2665\uFE0E" // Because lol: https://code.google.com/p/android/issues/detail?id=231068
                    to post.virtuals.recommends,
                timestamp = post.createdAt,
                author = user.name,
                commentCount = post.virtuals.responsesCreatedCount,
                tag = collection?.name,
                itemClickUrl = url,
                itemCommentClickUrl = constructCommentsUrl(),
                summarizationInfo = SummarizationInfo.from(url)
            )
          }
        }
        .toList()
        .map { DataResult(it, null) }
        .toMaybe()
  }

  override fun linkHandler() = linkHandler
}

@Meta
@ServiceModule
@Module
abstract class MediumMetaModule {

  @IntoMap
  @ServiceMetaKey(SERVICE_KEY)
  @Binds
  internal abstract fun mediumServiceMeta(@InternalApi meta: ServiceMeta): ServiceMeta

  @Module
  companion object {

    @InternalApi
    @Provides
    @Reusable
    @JvmStatic
    internal fun provideMediumServiceMeta() = ServiceMeta(
        SERVICE_KEY,
        R.string.medium,
        R.color.mediumAccent,
        R.drawable.logo_medium,
        firstPageKey = ""
    )
  }
}

@ServiceModule
@Module(includes = [MediumMetaModule::class])
abstract class MediumModule {

  @IntoMap
  @ServiceKey(SERVICE_KEY)
  @Binds
  internal abstract fun mediumService(mediumService: MediumService): Service

  @Module
  companion object {

    @Provides
    @InternalApi
    @JvmStatic
    internal fun provideMediumOkHttpClient(client: OkHttpClient): OkHttpClient {
      return client.newBuilder()
          .addInterceptor { chain ->
            chain.proceed(chain.request().newBuilder()
                // Tack format=json to the end
                .url(chain.request().url()
                    .newBuilder()
                    .addQueryParameter("format", "json")
                    .build())
                .build())
                .apply {
                  body()?.source()?.let {
                    // Medium prefixes with a while loop to prevent javascript eval attacks, so
                    // skip to the first open curly brace
                    it.skip(it.indexOf('{'.toByte()))
                  }
                }
          }
          .build()
    }

    @Provides
    @InternalApi
    @JvmStatic
    internal fun provideMediumMoshi(moshi: Moshi): Moshi {
      return moshi.newBuilder()
          .add(Instant::class.java, EpochInstantJsonAdapter(MILLISECONDS))
          .build()
    }

    @Provides
    @JvmStatic
    internal fun provideInspector() = Inspector.Builder().build()

    @Provides
    @JvmStatic
    internal fun provideMediumService(@InternalApi client: Lazy<OkHttpClient>,
        @InternalApi moshi: Moshi,
        inspectorConverterFactory: InspectorConverterFactory,
        rxJavaCallAdapterFactory: RxJava2CallAdapterFactory): MediumApi {
      val retrofit = Retrofit.Builder().baseUrl(MediumApi.ENDPOINT)
          .callFactory { client.get().newCall(it) }
          .addCallAdapterFactory(rxJavaCallAdapterFactory)
          .addConverterFactory(inspectorConverterFactory)
          .addConverterFactory(MoshiConverterFactory.create(moshi))
          .validateEagerly(BuildConfig.DEBUG)
          .build()
      return retrofit.create(MediumApi::class.java)
    }
  }
}
