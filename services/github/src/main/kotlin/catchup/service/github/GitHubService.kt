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
package catchup.service.github

import android.graphics.Color
import catchup.appconfig.AppConfig
import catchup.di.AppScope
import catchup.gemoji.EmojiMarkdownConverter
import catchup.gemoji.replaceMarkdownEmojisIn
import catchup.libraries.retrofitconverters.DecodingConverter
import catchup.libraries.retrofitconverters.delegatingCallFactory
import catchup.service.api.CatchUpItem
import catchup.service.api.ContentType
import catchup.service.api.DataRequest
import catchup.service.api.DataResult
import catchup.service.api.Service
import catchup.service.api.ServiceKey
import catchup.service.api.ServiceMeta
import catchup.service.api.ServiceMetaKey
import catchup.service.api.TextService
import catchup.service.github.GitHubApi.Language.All
import catchup.service.github.GitHubApi.Since.DAILY
import catchup.service.github.model.SearchQuery
import catchup.service.github.model.TrendingTimespan
import catchup.service.github.type.LanguageOrder
import catchup.service.github.type.LanguageOrderField
import catchup.service.github.type.OrderDirection
import catchup.util.e
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Optional
import com.apollographql.apollo3.cache.http.HttpFetchPolicy.NetworkOnly
import com.apollographql.apollo3.cache.http.httpFetchPolicy
import com.apollographql.apollo3.exception.DefaultApolloException
import com.squareup.anvil.annotations.ContributesMultibinding
import com.squareup.anvil.annotations.ContributesTo
import dagger.Binds
import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import javax.inject.Inject
import javax.inject.Qualifier
import okhttp3.OkHttpClient
import retrofit2.Retrofit

@Qualifier private annotation class InternalApi

private const val SERVICE_KEY = "github"

@ServiceKey(SERVICE_KEY)
@ContributesMultibinding(AppScope::class, boundType = Service::class)
class GitHubService
@Inject
constructor(
  @InternalApi private val serviceMeta: ServiceMeta,
  private val apolloClient: Lazy<ApolloClient>,
  private val emojiMarkdownConverter: Lazy<EmojiMarkdownConverter>,
  private val gitHubApi: Lazy<GitHubApi>,
) : TextService {

  override fun meta() = serviceMeta

  override suspend fun fetch(request: DataRequest): DataResult {
    return try {
      fetchByScraping(request)
    } catch (t: Throwable) {
      e(t) { "GitHub trending scraping failed." }
      try {
        fetchByQuery(request)
      } catch (t: Throwable) {
        e(t) { "GitHub trending query failed." }
        DataResult(emptyList(), null)
      }
    }
  }

  private suspend fun fetchByScraping(request: DataRequest): DataResult {
    return gitHubApi
      .get()
      .getTrending(language = All, since = DAILY)
      .mapIndexed { index, it ->
        with(it) {
          CatchUpItem(
            id = "$author/$repoName".hashCode().toLong(),
            title = "$author/$repoName",
            description = description.trim().takeUnless { it.isBlank() },
            timestamp = null,
            score = "★" to stars,
            tag = language.takeUnless { it.isBlank() },
            tagHintColor = languageColor?.let(Color::parseColor),
            itemClickUrl = url,
            source = starsToday?.toString()?.let { "$it stars today" },
            // TODO include index
            indexInResponse = index + request.pageOffset,
            serviceId = meta().id,
            contentType = ContentType.OTHER, // Not summarizable
          )
        }
      }
      .let { DataResult(it, null) }
  }

  private suspend fun fetchByQuery(request: DataRequest): DataResult {
    val query =
      SearchQuery(createdSince = TrendingTimespan.WEEK.createdSince(), minStars = 50).toString()

    val searchQuery =
      apolloClient
        .get()
        .newBuilder()
        .httpFetchPolicy(NetworkOnly)
        .build()
        .query(
          GitHubSearchQuery(
            queryString = query,
            firstCount = 50,
            order = LanguageOrder(direction = OrderDirection.DESC, field = LanguageOrderField.SIZE),
            after = Optional.presentIfNotNull(request.pageKey),
          )
        )
        .execute()

    if (searchQuery.hasErrors()) {
      throw DefaultApolloException(searchQuery.errors.toString())
    }
    return searchQuery.data!!.let { (search) ->
      search.nodes
        ?.mapNotNull { it?.onRepository }
        .orEmpty()
        .mapIndexed { index, it ->
          with(it) {
            val description =
              description
                ?.let { " — ${emojiMarkdownConverter.get().replaceMarkdownEmojisIn(it)}" }
                .orEmpty()

            CatchUpItem(
              id = id.hashCode().toLong(),
              title = "${owner.login} / $name",
              description = description,
              score = "★" to stargazers.totalCount,
              timestamp = createdAt,
              author = owner.login,
              tag = languages?.nodes?.firstOrNull()?.name,
              source = licenseInfo?.name,
              itemClickUrl = url.toString(),
              indexInResponse = index + request.pageOffset,
              serviceId = meta().id,
              contentType = ContentType.OTHER, // Not summarizable
            )
          }
        }
        .let {
          if (search.pageInfo.hasNextPage) {
            DataResult(it, search.pageInfo.endCursor)
          } else {
            DataResult(it, null)
          }
        }
    }
  }
}

@ContributesTo(AppScope::class)
@Module
abstract class GitHubMetaModule {

  @IntoMap
  @ServiceMetaKey(SERVICE_KEY)
  @Binds
  internal abstract fun githubServiceMeta(@InternalApi real: ServiceMeta): ServiceMeta

  companion object {

    @InternalApi
    @Provides
    fun provideGitHubServiceMeta(): ServiceMeta =
      ServiceMeta(
        SERVICE_KEY,
        R.string.catchup_service_github_name,
        R.color.catchup_service_github_accent,
        R.drawable.catchup_service_github_logo,
        firstPageKey = null,
        enabled = BuildConfig.GITHUB_DEVELOPER_TOKEN.run { !isNullOrEmpty() && !equals("null") },
      )
  }
}

@ContributesTo(AppScope::class)
@Module(includes = [GitHubMetaModule::class])
object GitHubModule {

  @Provides
  fun provideGitHubService(client: Lazy<OkHttpClient>, appConfig: AppConfig): GitHubApi {
    return Retrofit.Builder()
      .baseUrl(GitHubApi.ENDPOINT)
      .delegatingCallFactory(client)
      .addConverterFactory(DecodingConverter.newFactory(GitHubTrendingParser::parse))
      .validateEagerly(appConfig.isDebug)
      .build()
      .create(GitHubApi::class.java)
  }
}
