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
package io.sweers.catchup.data

import android.content.Context
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.CompiledField
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.Executable.Variables
import com.apollographql.apollo3.api.http.DefaultHttpRequestComposer
import com.apollographql.apollo3.api.http.HttpRequestComposer
import com.apollographql.apollo3.cache.http.CachingHttpEngine
import com.apollographql.apollo3.cache.http.DiskLruHttpCache
import com.apollographql.apollo3.cache.normalized.CacheKey
import com.apollographql.apollo3.cache.normalized.CacheKeyResolver
import com.apollographql.apollo3.cache.normalized.MemoryCacheFactory
import com.apollographql.apollo3.cache.normalized.NormalizedCacheFactory
import com.apollographql.apollo3.cache.normalized.ObjectIdGenerator
import com.apollographql.apollo3.cache.normalized.ObjectIdGeneratorContext
import com.apollographql.apollo3.cache.normalized.withNormalizedCache
import com.apollographql.apollo3.network.NetworkTransport
import com.apollographql.apollo3.network.http.DefaultHttpEngine
import com.apollographql.apollo3.network.http.HttpEngine
import com.apollographql.apollo3.network.http.HttpNetworkTransport
import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.sweers.catchup.BuildConfig
import io.sweers.catchup.data.github.type.Types
import io.sweers.catchup.util.injection.qualifiers.ApplicationContext
import io.sweers.catchup.util.network.AuthInterceptor
import okhttp3.OkHttpClient
import javax.inject.Qualifier
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
internal object GithubApolloModule {

  private const val SERVER_URL = "https://api.github.com/graphql"

  @Qualifier
  private annotation class InternalApi

  /**
   * TODO this hits disk on startup -_-
   */
  @Provides
  @Singleton
  internal fun provideHttpCacheStore(@ApplicationContext context: Context): DiskLruHttpCache =
    DiskLruHttpCache(context.cacheDir, 1_000_000)

  @Provides
  @InternalApi
  @Singleton
  internal fun provideGitHubOkHttpClient(
    client: OkHttpClient
  ): OkHttpClient = client.newBuilder()
    .addInterceptor(AuthInterceptor("token", BuildConfig.GITHUB_DEVELOPER_TOKEN))
    .build()

  @Provides
  @Singleton
  internal fun provideObjectIdGenerator(): ObjectIdGenerator = object : ObjectIdGenerator {
    private val formatter = { id: String ->
      if (id.isEmpty()) {
        null
      } else {
        CacheKey.from(id)
      }
    }

    override fun cacheKeyForObject(
      obj: Map<String, Any?>,
      context: ObjectIdGeneratorContext
    ): CacheKey? {
      // Most objects use id
      obj["id"].let {
        return when (val value = it) {
          is String -> formatter(value)
          else -> null
        }
      }
    }
  }

  @Provides
  @Singleton
  internal fun provideCacheKeyResolver(): CacheKeyResolver = object : CacheKeyResolver() {
    override fun cacheKeyForField(field: CompiledField, variables: Variables): CacheKey? {
      return null
    }
  }

  @Provides
  @Singleton
  internal fun provideNormalizedCacheFactory(): NormalizedCacheFactory =
    MemoryCacheFactory()

  @Provides
  @Singleton
  internal fun provideHttpEngine(
    @InternalApi client: Lazy<OkHttpClient>,
    @ApplicationContext context: Context,
  ): HttpEngine {
    val delegate = DefaultHttpEngine { client.get().newCall(it) }
    return CachingHttpEngine(
      directory = context.cacheDir,
      maxSize = 1_000_000,
      delegate = delegate
    )
  }

  @Provides
  @Singleton
  internal fun provideHttpRequestComposer(): HttpRequestComposer {
    return DefaultHttpRequestComposer(SERVER_URL)
  }

  @Provides
  @Singleton
  internal fun provideNetworkTransport(
    httpEngine: HttpEngine,
    httpRequestComposer: HttpRequestComposer
  ): NetworkTransport {
    return HttpNetworkTransport(httpRequestComposer, httpEngine)
  }

  @Provides
  @Singleton
  internal fun provideApolloClient(
    networkTransport: NetworkTransport,
    cacheFactory: NormalizedCacheFactory,
    cacheKeyResolver: CacheKeyResolver,
    objectIdGenerator: ObjectIdGenerator
  ): ApolloClient {
    return ApolloClient(
      networkTransport = networkTransport,
      customScalarAdapters = CustomScalarAdapters(
        mapOf(
          Types.DateTime.name to ISO8601InstantApolloAdapter,
          Types.URI.name to HttpUrlApolloAdapter,
        )
      ),
    ).withNormalizedCache(
      cacheFactory, cacheResolver = cacheKeyResolver,
      objectIdGenerator = objectIdGenerator
    )
  }
}
