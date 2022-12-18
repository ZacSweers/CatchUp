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
package io.sweers.catchup.service.medium

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
import io.sweers.catchup.libraries.retrofitconverters.delegatingCallFactory
import io.sweers.catchup.service.api.CatchUpItem
import io.sweers.catchup.service.api.DataRequest
import io.sweers.catchup.service.api.DataResult
import io.sweers.catchup.service.api.Mark.Companion.createCommentMark
import io.sweers.catchup.service.api.Service
import io.sweers.catchup.service.api.ServiceKey
import io.sweers.catchup.service.api.ServiceMeta
import io.sweers.catchup.service.api.ServiceMetaKey
import io.sweers.catchup.service.api.SummarizationInfo
import io.sweers.catchup.service.api.TextService
import io.sweers.catchup.service.medium.model.MediumPost
import io.sweers.catchup.util.data.adapters.EpochInstantJsonAdapter
import java.util.concurrent.TimeUnit.MILLISECONDS
import javax.inject.Inject
import javax.inject.Qualifier
import kotlinx.datetime.Instant
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory

@Qualifier private annotation class InternalApi

private const val SERVICE_KEY = "medium"

@ServiceKey(SERVICE_KEY)
@ContributesMultibinding(AppScope::class, boundType = Service::class)
class MediumService
@Inject
constructor(@InternalApi private val serviceMeta: ServiceMeta, private val api: MediumApi) :
  TextService {

  override fun meta() = serviceMeta

  override suspend fun fetch(request: DataRequest): DataResult {
    return api
      .top()
      .flatMap { references ->
        references.post.values.map { post ->
          MediumPost(
            post = post,
            user = references.user[post.creatorId]
                ?: throw IllegalStateException("Missing user on post!"),
            collection = references.collection?.get(post.homeCollectionId)
          )
        }
      }
      .mapIndexed { index, it ->
        with(it) {
          val url = constructUrl()
          CatchUpItem(
            id = post.id.hashCode().toLong(),
            title = post.title,
            score = "\u2665\uFE0E" // Because lol:
              // https://code.google.com/p/android/issues/detail?id=231068
              to post.virtuals.recommends,
            timestamp = post.createdAt,
            author = user.name,
            tag = collection?.name,
            source = "\u2605".takeIf { post.isSubscriptionLocked }, // TODO use "★" directly?
            itemClickUrl = url,
            summarizationInfo = SummarizationInfo.from(url),
            mark =
              createCommentMark(
                count = post.virtuals.responsesCreatedCount,
                clickUrl = constructCommentsUrl()
              ),
            indexInResponse = index + request.pageOffset,
            serviceId = meta().id,
          )
        }
      }
      .let { DataResult(it, null) }
  }
}

@ContributesTo(AppConfig::class)
@Module
abstract class MediumMetaModule {

  @IntoMap
  @ServiceMetaKey(SERVICE_KEY)
  @Binds
  internal abstract fun mediumServiceMeta(@InternalApi meta: ServiceMeta): ServiceMeta

  companion object {

    @InternalApi
    @Provides
    @Reusable
    internal fun provideMediumServiceMeta(): ServiceMeta =
      ServiceMeta(
        SERVICE_KEY,
        R.string.medium,
        R.color.mediumAccent,
        R.drawable.logo_medium,
        firstPageKey = null
      )
  }
}

@ContributesTo(AppScope::class)
@Module(includes = [MediumMetaModule::class])
object MediumModule {

  @Provides
  @InternalApi
  internal fun provideMediumOkHttpClient(client: OkHttpClient): OkHttpClient {
    return client
      .newBuilder()
      .addInterceptor { chain ->
        chain
          .proceed(
            chain
              .request()
              .newBuilder()
              // Tack format=json to the end
              .url(chain.request().url.newBuilder().addQueryParameter("format", "json").build())
              .build()
          )
          .apply {
            body.source().let {
              // Medium prefixes with a while loop to prevent javascript eval attacks, so
              // skip to the first open curly brace
              it.skip(it.indexOf('{'.code.toByte()))
            }
          }
      }
      .build()
  }

  @Provides
  @InternalApi
  internal fun provideMediumMoshi(moshi: Moshi): Moshi {
    return moshi
      .newBuilder()
      .add(Instant::class.java, EpochInstantJsonAdapter(MILLISECONDS))
      .build()
  }

  @Provides
  internal fun provideMediumService(
    @InternalApi client: Lazy<OkHttpClient>,
    @InternalApi moshi: Moshi,
    rxJavaCallAdapterFactory: RxJava3CallAdapterFactory,
    appConfig: AppConfig
  ): MediumApi {
    val retrofit =
      Retrofit.Builder()
        .baseUrl(MediumApi.ENDPOINT)
        .delegatingCallFactory(client)
        .addCallAdapterFactory(rxJavaCallAdapterFactory)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .validateEagerly(appConfig.isDebug)
        .build()
    return retrofit.create(MediumApi::class.java)
  }
}
