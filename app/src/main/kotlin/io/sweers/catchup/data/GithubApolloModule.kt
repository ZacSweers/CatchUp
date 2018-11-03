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

package io.sweers.catchup.data

import android.content.Context
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.cache.http.HttpCache
import com.apollographql.apollo.api.cache.http.HttpCacheStore
import com.apollographql.apollo.cache.http.ApolloHttpCache
import com.apollographql.apollo.cache.http.DiskLruHttpCacheStore
import com.apollographql.apollo.cache.normalized.CacheKey
import com.apollographql.apollo.cache.normalized.CacheKeyResolver
import com.apollographql.apollo.cache.normalized.NormalizedCacheFactory
import com.apollographql.apollo.cache.normalized.lru.EvictionPolicy
import com.apollographql.apollo.cache.normalized.lru.LruNormalizedCacheFactory
import dagger.Lazy
import dagger.Module
import dagger.Provides
import io.sweers.catchup.BuildConfig
import io.sweers.catchup.data.github.type.CustomType
import io.sweers.catchup.util.injection.qualifiers.ApplicationContext
import io.sweers.catchup.util.network.AuthInterceptor
import okhttp3.OkHttpClient
import javax.inject.Qualifier
import javax.inject.Singleton

@Module
internal object GithubApolloModule {

  private const val SERVER_URL = "https://api.github.com/graphql"

  @Qualifier
  private annotation class InternalApi

  @Provides
  @JvmStatic
  @Singleton
  internal fun provideHttpCacheStore(@ApplicationContext context: Context): HttpCacheStore =
      DiskLruHttpCacheStore(context.cacheDir, 1_000_000)

  @Provides
  @JvmStatic
  @Singleton
  internal fun provideHttpCache(httpCacheStore: HttpCacheStore): HttpCache =
      ApolloHttpCache(httpCacheStore, null)

  @Provides
  @InternalApi
  @JvmStatic
  @Singleton
  internal fun provideGitHubOkHttpClient(
      client: OkHttpClient,
      httpCache: HttpCache): OkHttpClient = client.newBuilder()
      .addInterceptor(httpCache.interceptor())
      .addInterceptor(AuthInterceptor("token", BuildConfig.GITHUB_DEVELOPER_TOKEN))
      .build()

  @Provides
  @JvmStatic
  @Singleton
  internal fun provideCacheKeyResolver(): CacheKeyResolver = object : CacheKeyResolver() {
    private val formatter = { id: String ->
      if (id.isEmpty()) {
        CacheKey.NO_KEY
      } else {
        CacheKey.from(id)
      }
    }

    override fun fromFieldRecordSet(field: ResponseField,
        objectSource: Map<String, Any>): CacheKey =// Most objects use id
        objectSource["id"].let {
          return when (val value = it) {
            is String -> formatter(value)
            else -> CacheKey.NO_KEY
          }
        }

    override fun fromFieldArguments(field: ResponseField,
        variables: Operation.Variables): CacheKey = CacheKey.NO_KEY
  }

  @Provides
  @JvmStatic
  @Singleton
  internal fun provideNormalizedCacheFactory(): NormalizedCacheFactory<*> =
      LruNormalizedCacheFactory(EvictionPolicy.NO_EVICTION)

  @Provides
  @JvmStatic
  @Singleton
  internal fun provideApolloClient(@InternalApi client: Lazy<OkHttpClient>,
      cacheFactory: NormalizedCacheFactory<*>,
      resolver: CacheKeyResolver,
      httpCache: HttpCache): ApolloClient {
    val instantAdapter = ISO8601InstantApolloAdapter()
    val httpUrlAdapter = HttpUrlApolloAdapter()
    return ApolloClient.builder()
        .serverUrl(SERVER_URL)
        .httpCache(httpCache)
        .callFactory { client.get().newCall(it) }
        .normalizedCache(cacheFactory, resolver)
        .addCustomTypeAdapter(CustomType.DATETIME, instantAdapter)
        .addCustomTypeAdapter(CustomType.URI, httpUrlAdapter)
        .addCustomTypeAdapter(CustomType.DATETIME, instantAdapter)
        .addCustomTypeAdapter(CustomType.URI, httpUrlAdapter)
        .build()
  }
}
