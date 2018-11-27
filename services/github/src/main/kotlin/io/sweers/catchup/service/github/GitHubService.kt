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

package io.sweers.catchup.service.github

import android.graphics.Color
import com.apollographql.apollo.ApolloCall
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Input
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.api.cache.http.HttpCachePolicy
import com.apollographql.apollo.exception.ApolloException
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import dagger.Binds
import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.Reusable
import dagger.multibindings.IntoMap
import io.sweers.catchup.gemoji.EmojiMarkdownConverter
import io.sweers.catchup.gemoji.replaceMarkdownEmojis
import io.sweers.catchup.libraries.retrofitconverters.DecodingConverter
import io.sweers.catchup.service.api.CatchUpItem
import io.sweers.catchup.service.api.DataRequest
import io.sweers.catchup.service.api.DataResult
import io.sweers.catchup.service.api.LinkHandler
import io.sweers.catchup.service.api.Mark
import io.sweers.catchup.service.api.Service
import io.sweers.catchup.service.api.ServiceKey
import io.sweers.catchup.service.api.ServiceMeta
import io.sweers.catchup.service.api.ServiceMetaKey
import io.sweers.catchup.service.api.TextService
import io.sweers.catchup.service.github.GitHubApi.Language.All
import io.sweers.catchup.service.github.GitHubApi.Since.DAILY
import io.sweers.catchup.service.github.GitHubSearchQuery.AsRepository
import io.sweers.catchup.service.github.model.SearchQuery
import io.sweers.catchup.service.github.model.TrendingTimespan
import io.sweers.catchup.service.github.type.LanguageOrder
import io.sweers.catchup.service.github.type.LanguageOrderField
import io.sweers.catchup.service.github.type.OrderDirection
import io.sweers.catchup.serviceregistry.annotations.Meta
import io.sweers.catchup.serviceregistry.annotations.ServiceModule
import io.sweers.catchup.util.e
import io.sweers.catchup.util.kotlin.concatMapEager
import io.sweers.catchup.util.nullIfBlank
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import javax.inject.Inject
import javax.inject.Qualifier
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Qualifier
private annotation class InternalApi

private const val SERVICE_KEY = "github"

internal class GitHubService @Inject constructor(
    @InternalApi private val serviceMeta: ServiceMeta,
    private val apolloClient: Lazy<ApolloClient>,
    private val emojiMarkdownConverter: Lazy<EmojiMarkdownConverter>,
    private val gitHubApi: Lazy<GitHubApi>,
    private val linkHandler: LinkHandler)
  : TextService {

  override fun meta() = serviceMeta

  override suspend fun fetchPage(request: DataRequest): DataResult {
    return try {
      fetchByScraping()
    } catch (t: Throwable) {
      e(t) { "GitHub trending scraping failed." }
      fetchByQuery(request)
    }
  }

  override fun linkHandler() = linkHandler

  private suspend fun fetchByScraping() = withContext(Dispatchers.Default) {
    gitHubApi
        .get()
        .getTrending(language = All, since = DAILY)
        .await()
        .concatMapEager { trendingItem ->
          with(trendingItem) {
            CatchUpItem(
                id = "$author/$repoName".hashCode().toLong(),
                title = "$repoName — $description",
                author = author,
                timestamp = null,
                score = "★" to stars,
                tag = language,
                itemClickUrl = url,
                mark = starsToday?.let { starCount ->
                  Mark(text = starCount.toString(),
                      textPrefix = "+",
                      icon = R.drawable.ic_star_black_24dp,
                      iconTintColor = languageColor?.let(Color::parseColor)
                  )
                }
            )
          }
        }
        .let { DataResult(it, null) }
  }

  // Adapted with cancellation from https://github.com/apollographql/apollo-android/issues/606#issuecomment-354562134
  private suspend fun <T> ApolloCall<T>.execute() = suspendCancellableCoroutine<Response<T>> { cont ->
    cont.invokeOnCancellation { cancel() }
    enqueue(object : ApolloCall.Callback<T>() {
      override fun onResponse(response: Response<T>) {
        cont.resume(response)
      }

      override fun onFailure(e: ApolloException) {
        cont.resumeWithException(e)
      }
    })
  }

  private suspend fun fetchByQuery(request: DataRequest) = withContext(Dispatchers.Default) {
    val query = SearchQuery(
        createdSince = TrendingTimespan.WEEK.createdSince(),
        minStars = 50)
        .toString()

    val searchQuery = apolloClient.get().query(GitHubSearchQuery(query,
        50,
        LanguageOrder.builder()
            .direction(OrderDirection.DESC)
            .field(LanguageOrderField.SIZE)
            .build(),
        Input.fromNullable(request.pageId.nullIfBlank())))
        .httpCachePolicy(HttpCachePolicy.NETWORK_ONLY)

    val response = searchQuery.execute()
    if (response.hasErrors()) {
      throw ApolloException(response.errors().toString())
    }
    response.data()?.let { data ->
      data.search().nodes().orEmpty()
          .filterIsInstance<AsRepository>()
          .concatMapEager { repository ->
            with(repository) {
              val description = description()
                  ?.let { " — ${replaceMarkdownEmojis(it, emojiMarkdownConverter.get())}" }
                  .orEmpty()

              CatchUpItem(
                  id = id().hashCode().toLong(),
                  title = "${name()}$description",
                  score = "★" to stargazers().totalCount(),
                  timestamp = createdAt(),
                  author = owner().login(),
                  tag = languages()?.nodes()?.firstOrNull()?.name(),
                  source = licenseInfo()?.name(),
                  itemClickUrl = url().toString()
              )
            }
          }
          .let {
            if (data.search().pageInfo().hasNextPage()) {
              DataResult(it, data.search().pageInfo().endCursor())
            } else {
              DataResult(it, null)
            }
          }
    } ?: throw ApolloException("Null data")
  }
}

@Meta
@ServiceModule
@Module
abstract class GitHubMetaModule {

  @IntoMap
  @ServiceMetaKey(SERVICE_KEY)
  @Binds
  internal abstract fun githubServiceMeta(@InternalApi meta: ServiceMeta): ServiceMeta

  @Module
  companion object {

    @InternalApi
    @Provides
    @Reusable
    @JvmStatic
    internal fun provideGitHubServiceMeta() = ServiceMeta(
        SERVICE_KEY,
        R.string.github,
        R.color.githubAccent,
        R.drawable.logo_github,
        firstPageKey = "",
        enabled = BuildConfig.GITHUB_DEVELOPER_TOKEN.run { !isNullOrEmpty() && !equals("null") }
    )
  }
}

@ServiceModule
@Module(includes = [GitHubMetaModule::class])
abstract class GitHubModule {

  @IntoMap
  @ServiceKey(SERVICE_KEY)
  @Binds
  internal abstract fun githubService(githubService: GitHubService): Service

  @Module
  companion object {
    @Provides
    @JvmStatic
    internal fun provideGitHubService(client: Lazy<OkHttpClient>): GitHubApi {
      return Retrofit.Builder().baseUrl(GitHubApi.ENDPOINT)
          .callFactory { client.get().newCall(it) }
          .addCallAdapterFactory(CoroutineCallAdapterFactory())
          .addConverterFactory(DecodingConverter.newFactory(GitHubTrendingParser::parse))
          .validateEagerly(BuildConfig.DEBUG)
          .build()
          .create(GitHubApi::class.java)
    }
  }

}
