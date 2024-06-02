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
package catchup.service.reddit

import catchup.appconfig.AppConfig
import catchup.di.AppScope
import catchup.libraries.retrofitconverters.delegatingCallFactory
import catchup.service.api.CatchUpItem
import catchup.service.api.Comment
import catchup.service.api.ContentType
import catchup.service.api.DataRequest
import catchup.service.api.DataResult
import catchup.service.api.Detail
import catchup.service.api.Mark.Companion.createCommentMark
import catchup.service.api.Service
import catchup.service.api.ServiceKey
import catchup.service.api.ServiceMeta
import catchup.service.api.ServiceMetaKey
import catchup.service.api.TextService
import catchup.service.reddit.model.RedditComment
import catchup.service.reddit.model.RedditLink
import catchup.service.reddit.model.RedditListing
import catchup.service.reddit.model.RedditObjectFactory
import catchup.util.data.adapters.EpochInstantJsonAdapter
import com.squareup.anvil.annotations.ContributesMultibinding
import com.squareup.anvil.annotations.ContributesTo
import com.squareup.moshi.Moshi
import dagger.Binds
import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import javax.inject.Inject
import javax.inject.Qualifier
import kotlinx.collections.immutable.toImmutableList
import kotlinx.datetime.Instant
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

@Qualifier private annotation class InternalApi

private const val SERVICE_KEY = "reddit"

@ServiceKey(SERVICE_KEY)
@ContributesMultibinding(AppScope::class, boundType = Service::class)
class RedditService
@Inject
constructor(@InternalApi private val serviceMeta: ServiceMeta, private val api: RedditApi) :
  TextService {

  override fun meta() = serviceMeta

  override suspend fun fetch(request: DataRequest): DataResult {
    // We special case the front page
    return api.frontPage(request.limit, request.pageKey).let { redditListingRedditResponse ->
      @Suppress("UNCHECKED_CAST")
      val data =
        (redditListingRedditResponse.data.children as List<RedditLink>).mapIndexed { index, link ->
          CatchUpItem(
            id = link.id.hashCode().toLong(),
            title = link.title,
            score = "+" to link.score,
            timestamp = link.createdUtc,
            author = "/u/" + link.author,
            source = link.domain ?: "self",
            tag = link.subreddit,
            itemClickUrl = link.url,
            mark =
              createCommentMark(
                count = link.commentsCount,
                clickUrl = "https://reddit.com/comments/${link.id}",
              ),
            indexInResponse = index + request.pageOffset,
            serviceId = meta().id,
            // If it's a selftext, mark it as HTML for summarizing.
            contentType = if (link.isSelf) ContentType.HTML else null,
            detailKey = link.id,
          )
        }
      DataResult(data, redditListingRedditResponse.data.after)
    }
  }

  override suspend fun fetchDetail(item: CatchUpItem, detailKey: String): Detail {
    val (listing, redditComments) =
      api
        .comments(
          id = item.detailKey!!,
          // TODO need a better way for this
          subreddit = item.tag!!.removePrefix("/r/"),
        )
        .map { it.data.children }

    val detailListing = listing[0] as RedditLink

    val comments =
      redditComments
        .filterIsInstance<RedditComment>()
        .flatMap { comment ->
          buildList {
            fun recurseComment(comment: RedditComment) {
              add(comment.toComment())
              comment.replies?.let { repliesListing ->
                if (repliesListing is RedditListing) {
                  repliesListing.children.filterIsInstance<RedditComment>().forEach {
                    recurseComment(it)
                  }
                }
              }
            }
            recurseComment(comment)
          }
        }
        .toImmutableList()

    return Detail.Full(
      id = detailListing.id,
      itemId = item.id,
      title = detailListing.title,
      text = detailListing.selftext,
      imageUrl = if (detailListing.postHint == "image") detailListing.url else null,
      score = detailListing.score.toLong(),
      shareUrl = detailListing.url,
      linkUrl = detailListing.url.takeUnless { detailListing.isSelf },
      comments = comments,
      commentsCount = detailListing.commentsCount,
      timestamp = detailListing.createdUtc,
      author = "/u/" + detailListing.author,
      tag = detailListing.subreddit,
      allowUnfurl = !detailListing.isSelf,
    )
  }
}

@ContributesTo(AppScope::class)
@Module
abstract class RedditMetaModule {

  @IntoMap
  @ServiceMetaKey(SERVICE_KEY)
  @Binds
  internal abstract fun redditServiceMeta(@InternalApi meta: ServiceMeta): ServiceMeta

  companion object {

    @InternalApi
    @Provides
    fun provideRedditServiceMeta(): ServiceMeta =
      ServiceMeta(
        SERVICE_KEY,
        R.string.catchup_service_reddit_name,
        R.color.catchup_service_reddit_accent,
        R.drawable.catchup_service_reddit_logo,
        firstPageKey = null,
      )
  }
}

@ContributesTo(AppScope::class)
@Module(includes = [RedditMetaModule::class])
object RedditModule {

  @InternalApi
  @Provides
  fun provideMoshi(upstreamMoshi: Moshi): Moshi {
    return upstreamMoshi
      .newBuilder()
      .add(RedditObjectFactory.INSTANCE)
      .add(Instant::class.java, EpochInstantJsonAdapter())
      .build()
  }

  @InternalApi
  @Provides
  fun provideRedditOkHttpClient(client: OkHttpClient): OkHttpClient {
    return client
      .newBuilder()
      .addNetworkInterceptor { chain ->
        var request = chain.request()
        val url = request.url
        request =
          request
            .newBuilder()
            .header("User-Agent", "CatchUp app by /u/pandanomic")
            .url(
              url
                .newBuilder()
                .encodedPath("${url.encodedPath}.json")
                .addQueryParameter("raw_json", "1") // So tokens aren't escaped
                .build()
            )
            .build()
        chain.proceed(request)
      }
      .build()
  }

  @Provides
  fun provideRedditApi(
    @InternalApi client: Lazy<OkHttpClient>,
    @InternalApi moshi: Moshi,
    appConfig: AppConfig,
  ): RedditApi {
    val retrofit =
      Retrofit.Builder()
        .baseUrl(RedditApi.ENDPOINT)
        .delegatingCallFactory(client)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .validateEagerly(appConfig.isDebug)
        .build()
    return retrofit.create(RedditApi::class.java)
  }
}

private fun RedditComment.toComment(): Comment {
  return Comment(
    id = id,
    serviceId = SERVICE_KEY,
    author = author,
    timestamp = createdUtc,
    text = body,
    score = score,
    children = emptyList(),
    depth = depth,
    clickableUrls = emptyList(),
  )
}
