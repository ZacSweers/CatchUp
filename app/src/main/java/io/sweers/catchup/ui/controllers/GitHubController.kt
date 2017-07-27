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
import android.support.v4.util.Pair
import android.view.ContextThemeWrapper
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.internal.Optional
import com.apollographql.apollo.cache.http.DiskLruHttpCacheStore
import com.apollographql.apollo.cache.http.HttpCache
import com.apollographql.apollo.cache.http.HttpCachePolicy
import com.apollographql.apollo.cache.http.HttpCacheStore
import com.apollographql.apollo.cache.normalized.CacheKey
import com.apollographql.apollo.cache.normalized.CacheKeyResolver
import com.apollographql.apollo.cache.normalized.NormalizedCacheFactory
import com.apollographql.apollo.cache.normalized.lru.EvictionPolicy
import com.apollographql.apollo.cache.normalized.lru.LruNormalizedCacheFactory
import com.apollographql.apollo.cache.normalized.sql.ApolloSqlHelper
import com.apollographql.apollo.cache.normalized.sql.SqlNormalizedCacheFactory
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
import io.sweers.catchup.data.github.TrendingTimespan
import io.sweers.catchup.data.github.model.Repository
import io.sweers.catchup.data.github.model.SearchQuery
import io.sweers.catchup.data.github.model.User
import io.sweers.catchup.data.github.type.CustomType
import io.sweers.catchup.data.github.type.LanguageOrder
import io.sweers.catchup.data.github.type.LanguageOrderField
import io.sweers.catchup.data.github.type.OrderDirection
import io.sweers.catchup.injection.qualifiers.ApplicationContext
import io.sweers.catchup.injection.scopes.PerController
import io.sweers.catchup.ui.base.BaseNewsController
import io.sweers.catchup.util.collect.emptyIfNull
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import org.threeten.bp.Instant
import java.util.concurrent.TimeUnit.DAYS
import javax.inject.Inject
import javax.inject.Qualifier

class GitHubController : BaseNewsController<CatchUpItem> {

  @Inject lateinit var apolloClient: ApolloClient
  @Inject lateinit var linkManager: LinkManager

  constructor() : super()

  constructor(args: Bundle) : super(args)

  override fun onThemeContext(context: Context): Context {
    return ContextThemeWrapper(context, R.style.CatchUp_GitHub)
  }

  override fun bindItemView(item: CatchUpItem, holder: BaseNewsController.NewsItemViewHolder) {
    holder.bind(this, item, linkManager)
  }

  override fun getDataSingle(request: BaseNewsController.DataRequest): Single<List<CatchUpItem>> {
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
            .build()))
        .httpCachePolicy(
            if (request.fromRefresh) {
              HttpCachePolicy.NETWORK_FIRST.expireAfter(1, DAYS)
            } else {
              HttpCachePolicy.CACHE_FIRST
            }
        )

    return Rx2Apollo.from(searchQuery)
        .firstOrError() // TODO Do we want to handle multiple?
        .map { it.data()!! }
        .flatMap { data ->
          Observable.fromIterable(data.search().nodes().emptyIfNull())
              .map { it.asRepository()!! }
              .map { node ->
                var primaryLanguage: String? = null
                val langs = node.languages()
                if (langs != null && langs.nodes() != null) {
                  val nodes = langs.nodes()
                  if (nodes != null && !nodes.isEmpty()) {
                    primaryLanguage = nodes[0].name()
                  }
                }
                Repository.builder()
                    .createdAt(node.createdAt())
                    .fullName(node.name())
                    .htmlUrl(node.url()
                        .toString())
                    .id(node.id()
                        .hashCode().toLong())
                    .language(primaryLanguage)
                    .name(node.name())
                    .owner(User.create(node.owner()
                        .login()))
                    .starsCount(node.stargazers()
                        .totalCount())
                    .build()
              }
              // Should probably combine these, but I also like having separation here
              .map {
                with(it) {
                  CatchUpItem.builder()
                      .id(id())
                      .hideComments(true)
                      .title(fullName())
                      .score(Pair.create("★", starsCount()))
                      .timestamp(createdAt())
                      .author(owner().login())
                      .tag(language())
                      .itemClickUrl(htmlUrl())
                      .build()
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
    internal fun provideCacheKeyResolver(): CacheKeyResolver {
      return object : CacheKeyResolver() {
        override fun fromFieldRecordSet(field: ResponseField,
            objectSource: Map<String, Any>): CacheKey {
          //Specific id for User type.
          if (objectSource["__typename"] == "User") {
            val userKey = objectSource["__typename"].toString() + "." + objectSource["login"]
            return CacheKey.from(userKey)
          }
          //Use id as default case.
          if (objectSource.containsKey("id")) {
            val typeNameAndIDKey = objectSource["__typename"].toString() + "." + objectSource["id"]
            return CacheKey.from(typeNameAndIDKey)
          }
          return CacheKey.NO_KEY
        }

        override fun fromFieldArguments(field: ResponseField,
            variables: Operation.Variables): CacheKey {
          return CacheKey.NO_KEY
        }
      }
    }

    @Provides
    @JvmStatic
    @PerController
    internal fun provideNormalizedCacheFactory(
        @ApplicationContext context: Context): NormalizedCacheFactory<*> {
      val apolloSqlHelper = ApolloSqlHelper(context, "githubdb")
      return LruNormalizedCacheFactory(EvictionPolicy.NO_EVICTION,
          SqlNormalizedCacheFactory(apolloSqlHelper))
    }

    @Provides
    @JvmStatic
    @PerController
    internal fun provideApolloClient(@InternalApi client: Lazy<OkHttpClient>,
        cacheFactory: NormalizedCacheFactory<*>,
        resolver: CacheKeyResolver,
        httpCacheStore: HttpCacheStore): ApolloClient {
      return ApolloClient.builder()
          .serverUrl(SERVER_URL)
          .httpCacheStore(httpCacheStore)
          .okHttpClient(client.get())
//          .callFactory { client.get().newCall(it) }
          .normalizedCache(cacheFactory, resolver)
          .addCustomTypeAdapter<Instant>(CustomType.DATETIME, ISO8601InstantApolloAdapter())
          .addCustomTypeAdapter<HttpUrl>(CustomType.URI, HttpUrlApolloAdapter())
          .build()
    }
  }
}
