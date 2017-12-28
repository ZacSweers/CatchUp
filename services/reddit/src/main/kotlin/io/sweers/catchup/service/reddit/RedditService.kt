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

package io.sweers.catchup.service.reddit

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
import io.sweers.catchup.service.api.LinkHandler
import io.sweers.catchup.service.api.Service
import io.sweers.catchup.service.api.ServiceKey
import io.sweers.catchup.service.api.ServiceMeta
import io.sweers.catchup.service.api.ServiceMetaKey
import io.sweers.catchup.service.api.SummarizationInfo
import io.sweers.catchup.service.api.TextService
import io.sweers.catchup.service.reddit.model.RedditLink
import io.sweers.catchup.service.reddit.model.RedditObjectFactory
import io.sweers.catchup.util.data.adapters.EpochInstantJsonAdapter
import io.sweers.catchup.util.nullIfBlank
import okhttp3.OkHttpClient
import org.threeten.bp.Instant
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Inject
import javax.inject.Qualifier

@Qualifier
private annotation class InternalApi

private const val SERVICE_KEY = "reddit"

internal class RedditService @Inject constructor(
    @InternalApi private val serviceMeta: ServiceMeta,
    private val api: RedditApi,
    private val linkHandler: LinkHandler)
  : TextService {

  override fun meta() = serviceMeta

  override fun fetchPage(request: DataRequest): Maybe<DataResult> {
    // We special case the front page
    return api.frontPage(25, request.pageId.nullIfBlank())
        .map { redditListingRedditResponse ->
          @Suppress("UNCHECKED_CAST")
          val data = (redditListingRedditResponse.data()
              .children() as List<RedditLink>)
              .map {
                CatchUpItem(
                    id = it.id().hashCode().toLong(),
                    title = it.title(),
                    score = "+" to it.score(),
                    timestamp = it.createdUtc(),
                    author = "/u/" + it.author(),
                    source = it.domain() ?: "self",
                    commentCount = it.commentsCount(),
                    tag = it.subreddit(),
                    itemClickUrl = it.url(),
                    itemCommentClickUrl = "https://reddit.com/comments/${it.id()}",
                    summarizationInfo = SummarizationInfo.from(it.url(), it.selftext())
                )
              }
          DataResult(data, redditListingRedditResponse.data().after())
        }
  }

  override fun linkHandler() = linkHandler
}

@Module
abstract class RedditMetaModule {

  @IntoMap
  @ServiceMetaKey(SERVICE_KEY)
  @Binds
  internal abstract fun redditServiceMeta(@InternalApi meta: ServiceMeta): ServiceMeta

  @Module
  companion object {

    @InternalApi
    @Provides
    @Reusable
    @JvmStatic
    internal fun provideRedditServiceMeta() = ServiceMeta(
        SERVICE_KEY,
        R.string.reddit,
        R.color.redditAccent,
        R.drawable.logo_reddit,
        firstPageKey = ""
    )
  }
}

@Module(includes = [RedditMetaModule::class])
abstract class RedditModule {

  @IntoMap
  @ServiceKey(SERVICE_KEY)
  @Binds
  internal abstract fun redditService(redditService: RedditService): Service

  @Module
  companion object {

    @InternalApi
    @Provides
    @JvmStatic
    internal fun provideMoshi(upstreamMoshi: Moshi): Moshi {
      return upstreamMoshi.newBuilder()
          .add(RedditAdapterFactory.create())
          .add(RedditObjectFactory.INSTANCE)
          .add(Instant::class.java, EpochInstantJsonAdapter())
          .build()
    }

    @InternalApi
    @Provides
    @JvmStatic
    internal fun provideRedditOkHttpClient(
        client: OkHttpClient): OkHttpClient {
      return client.newBuilder()
          .addNetworkInterceptor { chain ->
            var request = chain.request()
            val url = request.url()
            request = request.newBuilder()
                .header("User-Agent", "CatchUp app by /u/pandanomic")
                .url(url.newBuilder()
                    .encodedPath(url.encodedPath() + ".json")
                    .addQueryParameter("raw_json", "1") // So tokens aren't escaped
                    .build())
                .build()
            chain.proceed(request)
          }
          .build()
    }

    @Provides
    @JvmStatic
    internal fun provideRedditApi(@InternalApi client: Lazy<OkHttpClient>,
        rxJavaCallAdapterFactory: RxJava2CallAdapterFactory,
        @InternalApi moshi: Moshi): RedditApi {
      val retrofit = Retrofit.Builder().baseUrl(RedditApi.ENDPOINT)
          .callFactory { client.get().newCall(it) }
          .addCallAdapterFactory(rxJavaCallAdapterFactory)
          .addConverterFactory(MoshiConverterFactory.create(moshi))
          .validateEagerly(BuildConfig.DEBUG)
          .build()
      return retrofit.create(RedditApi::class.java)
    }
  }
}
