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

package io.sweers.catchup.service.github

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Input
import com.apollographql.apollo.api.cache.http.HttpCachePolicy
import com.apollographql.apollo.exception.ApolloException
import com.apollographql.apollo.rx2.Rx2Apollo
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.Reusable
import dagger.multibindings.IntoMap
import io.reactivex.Maybe
import io.reactivex.Observable
import io.sweers.catchup.gemoji.EmojiMarkdownConverter
import io.sweers.catchup.gemoji.replaceMarkdownEmojis
import io.sweers.catchup.service.api.CatchUpItem
import io.sweers.catchup.service.api.DataRequest
import io.sweers.catchup.service.api.DataResult
import io.sweers.catchup.service.api.LinkHandler
import io.sweers.catchup.service.api.Service
import io.sweers.catchup.service.api.ServiceKey
import io.sweers.catchup.service.api.ServiceMeta
import io.sweers.catchup.service.api.ServiceMetaKey
import io.sweers.catchup.service.api.TextService
import io.sweers.catchup.service.github.GitHubSearchQuery.AsRepository
import io.sweers.catchup.service.github.model.SearchQuery
import io.sweers.catchup.service.github.model.TrendingTimespan
import io.sweers.catchup.service.github.type.LanguageOrder
import io.sweers.catchup.service.github.type.LanguageOrderField
import io.sweers.catchup.service.github.type.OrderDirection
import io.sweers.catchup.util.nullIfBlank
import javax.inject.Inject
import javax.inject.Qualifier

@Qualifier
private annotation class InternalApi

private const val SERVICE_KEY = "github"

internal class GitHubService @Inject constructor(
    @InternalApi private val serviceMeta: ServiceMeta,
    private val apolloClient: ApolloClient,
    private val emojiMarkdownConverter: EmojiMarkdownConverter,
    private val linkHandler: LinkHandler)
  : TextService {

  override fun meta() = serviceMeta

  override fun fetchPage(request: DataRequest): Maybe<DataResult> {
    val query = SearchQuery.builder()
        .createdSince(TrendingTimespan.WEEK.createdSince())
        .minStars(50)
        .build()
        .toString()

    val searchQuery = apolloClient.query(GitHubSearchQuery(query,
        50,
        LanguageOrder.builder()
            .direction(OrderDirection.DESC)
            .field(LanguageOrderField.SIZE)
            .build(),
        Input.fromNullable(request.pageId.nullIfBlank())))
        .httpCachePolicy(HttpCachePolicy.NETWORK_ONLY)

    return Rx2Apollo.from(searchQuery)
        .firstOrError()
        .doOnSuccess {
          if (it.hasErrors()) {
            throw ApolloException(it.errors().toString())
          }
        }
        .map { it.data()!! }
        .flatMap { data ->
          Observable.fromIterable(data.search().nodes().orEmpty())
              .cast(AsRepository::class.java)
              .map {
                with(it) {
                  val description = description()
                      ?.let { " — ${replaceMarkdownEmojis(it, emojiMarkdownConverter)}" }
                      .orEmpty()

                  CatchUpItem(
                      id = id().hashCode().toLong(),
                      hideComments = true,
                      title = "${name()}$description",
                      score = "★" to stargazers().totalCount().toInt(),
                      timestamp = createdAt(),
                      author = owner().login(),
                      tag = languages()?.nodes()?.firstOrNull()?.name(),
                      source = licenseInfo()?.name(),
                      itemClickUrl = url().toString()
                  )
                }
              }
              .toList()
              .map {
                if (data.search().pageInfo().hasNextPage()) {
                  DataResult(it, data.search().pageInfo().endCursor())
                } else {
                  DataResult(it, null)
                }
              }
        }
        .toMaybe()
  }

  override fun linkHandler() = linkHandler
}

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
        firstPageKey = ""
    )
  }
}

@Module(includes = [GitHubMetaModule::class])
abstract class GitHubModule {

  @IntoMap
  @ServiceKey(SERVICE_KEY)
  @Binds
  internal abstract fun githubService(githubService: GitHubService): Service

}
