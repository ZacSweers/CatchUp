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

package io.sweers.catchup.ui.controllers

import android.content.Context
import android.os.Bundle
import android.view.ContextThemeWrapper
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.internal.Optional
import com.apollographql.apollo.cache.http.DiskLruHttpCacheStore
import com.apollographql.apollo.cache.http.HttpCache
import com.apollographql.apollo.cache.http.HttpCachePolicy
import com.apollographql.apollo.cache.http.HttpCacheStore
import com.apollographql.apollo.internal.util.ApolloLogger
import com.apollographql.apollo.rx2.Rx2Apollo
import dagger.Lazy
import dagger.Provides
import dagger.Subcomponent
import dagger.android.AndroidInjector
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import io.sweers.catchup.BuildConfig
import io.sweers.catchup.R
import io.sweers.catchup.data.AuthInterceptor
import io.sweers.catchup.data.CatchUpItem
import io.sweers.catchup.data.HttpUrlApolloAdapter
import io.sweers.catchup.data.ISO8601InstantApolloAdapter
import io.sweers.catchup.data.LinkManager
import io.sweers.catchup.data.github.GitHubSearchQuery
import io.sweers.catchup.data.github.SearchQuery
import io.sweers.catchup.data.github.TrendingTimespan
import io.sweers.catchup.data.github.type.CustomType
import io.sweers.catchup.data.github.type.LanguageOrder
import io.sweers.catchup.data.github.type.LanguageOrderField
import io.sweers.catchup.data.github.type.OrderDirection
import io.sweers.catchup.injection.qualifiers.ApplicationContext
import io.sweers.catchup.injection.scopes.PerController
import io.sweers.catchup.ui.base.CatchUpItemViewHolder
import io.sweers.catchup.ui.base.StorageBackedNewsController
import io.sweers.catchup.util.collect.emptyIfNull
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import org.threeten.bp.Instant
import javax.inject.Inject
import javax.inject.Qualifier

class GitHubController : StorageBackedNewsController {

  @Inject lateinit var apolloClient: ApolloClient
  @Inject lateinit var linkManager: LinkManager

  // TODO Placeholder for a better solution later
//  private var endCursor: String? = null

  constructor() : super()

  constructor(args: Bundle) : super(args)

  override fun onThemeContext(context: Context): Context {
    return ContextThemeWrapper(context, R.style.CatchUp_GitHub)
  }

  override fun bindItemView(item: CatchUpItem, holder: CatchUpItemViewHolder) {
    holder.bind(this, item, linkManager)
  }

  override fun serviceType() = "gh"

  override fun getDataFromService(page: Int): Single<List<CatchUpItem>> {
    setMoreDataAvailable(false)
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
        null))
        .httpCachePolicy(HttpCachePolicy.NETWORK_ONLY)

    return Rx2Apollo.from(searchQuery)
        .firstOrError()
        .map { it.data()!! }
        .flatMap { data ->
          //          with(data.search().pageInfo()) {
//            endCursor = endCursor()
//            if (endCursor == null) {
//              setMoreDataAvailable(false)
//            }
//          }
          Observable.fromIterable(data.search().nodes().emptyIfNull())
              .map { it.asRepository()!! }
              .map {
                with(it) {
                  CatchUpItem(
                      id = id().hashCode().toLong(),
                      hideComments = true,
                      title = "${name()} — ${description()}",
                      score = kotlin.Pair("★", stargazers().totalCount()),
                      timestamp = createdAt(),
                      author = owner().login(),
                      tag = languages()?.nodes()?.firstOrNull()?.name(),
                      source = licenseInfo()?.name(),
                      itemClickUrl = url().toString()
                  )
                }
              }
              .toList()
        }
        .subscribeOn(Schedulers.io())
  }

  @PerController
  @Subcomponent(modules = arrayOf(Module::class))
  interface Component : AndroidInjector<GitHubController> {

    @Subcomponent.Builder
    abstract class Builder : AndroidInjector.Builder<GitHubController>()
  }

  @dagger.Module
  object Module {

    private val SERVER_URL = "https://api.github.com/graphql"

    @Qualifier
    private annotation class InternalApi

    @Provides
    @JvmStatic
    @PerController
    internal fun provideHttpCacheStore(@ApplicationContext context: Context): HttpCacheStore {
      return DiskLruHttpCacheStore(context.cacheDir, 1_000_000)
    }

    @Provides
    @JvmStatic
    @PerController
    internal fun provideHttpCache(httpCacheStore: HttpCacheStore): HttpCache {
      return HttpCache(httpCacheStore, ApolloLogger(Optional.absent()))
    }

    @Provides
    @InternalApi
    @JvmStatic
    @PerController
    internal fun provideGitHubOkHttpClient(
        client: OkHttpClient,
        httpCache: HttpCache): OkHttpClient {
      return client.newBuilder()
          .addInterceptor(httpCache.interceptor())
          .addInterceptor(AuthInterceptor.create("token", BuildConfig.GITHUB_DEVELOPER_TOKEN))
          .build()
    }

    @Provides
    @JvmStatic
    @PerController
    internal fun provideApolloClient(@InternalApi client: Lazy<OkHttpClient>,
        httpCacheStore: HttpCacheStore): ApolloClient {
      return ApolloClient.builder()
          .serverUrl(SERVER_URL)
          .httpCacheStore(httpCacheStore)
          .okHttpClient(client.get())
//          .callFactory { client.get().newCall(it) }
          .addCustomTypeAdapter<Instant>(CustomType.DATETIME, ISO8601InstantApolloAdapter())
          .addCustomTypeAdapter<HttpUrl>(CustomType.URI, HttpUrlApolloAdapter())
          .build()
    }
  }
}
