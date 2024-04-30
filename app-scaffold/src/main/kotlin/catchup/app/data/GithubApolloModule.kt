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
package catchup.app.data

import android.content.Context
import catchup.app.data.github.type.DateTime
import catchup.app.data.github.type.URI
import catchup.di.AppScope
import catchup.di.SingleIn
import catchup.util.apollo.HttpUrlApolloAdapter
import catchup.util.apollo.ISO8601InstantApolloAdapter
import catchup.util.injection.qualifiers.ApplicationContext
import catchup.util.network.AuthInterceptor
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.CompiledField
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.Executable.Variables
import com.apollographql.apollo3.api.http.DefaultHttpRequestComposer
import com.apollographql.apollo3.api.http.HttpRequestComposer
import com.apollographql.apollo3.cache.http.DiskLruHttpCache
import com.apollographql.apollo3.cache.normalized.api.CacheKey
import com.apollographql.apollo3.cache.normalized.api.CacheKeyGenerator
import com.apollographql.apollo3.cache.normalized.api.CacheKeyGeneratorContext
import com.apollographql.apollo3.cache.normalized.api.CacheKeyResolver
import com.apollographql.apollo3.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo3.cache.normalized.api.NormalizedCacheFactory
import com.apollographql.apollo3.cache.normalized.normalizedCache
import com.apollographql.apollo3.network.NetworkTransport
import com.apollographql.apollo3.network.http.DefaultHttpEngine
import com.apollographql.apollo3.network.http.HttpEngine
import com.apollographql.apollo3.network.http.HttpNetworkTransport
import com.squareup.anvil.annotations.ContributesTo
import dagger.Lazy
import dagger.Module
import dagger.Provides
import dev.zacsweers.catchup.app.scaffold.BuildConfig
import javax.inject.Qualifier
import okhttp3.OkHttpClient
import okio.FileSystem

@ContributesTo(AppScope::class)
@Module
object GithubApolloModule {

  private const val SERVER_URL = "https://api.github.com/graphql"

  @Qualifier private annotation class InternalApi

  /** TODO this hits disk on startup -_- */
  @Provides
  @SingleIn(AppScope::class)
  fun provideHttpCacheStore(@ApplicationContext context: Context): DiskLruHttpCache =
    DiskLruHttpCache(FileSystem.SYSTEM, context.cacheDir, 1_000_000)

  @Provides
  @InternalApi
  @SingleIn(AppScope::class)
  fun provideGitHubOkHttpClient(client: OkHttpClient): OkHttpClient =
    client
      .newBuilder()
      .addInterceptor(AuthInterceptor("Bearer", BuildConfig.GITHUB_DEVELOPER_TOKEN))
      .build()

  @Provides
  @SingleIn(AppScope::class)
  fun provideCacheKeyGenerator(): CacheKeyGenerator =
    object : CacheKeyGenerator {
      private val formatter = { id: String ->
        if (id.isEmpty()) {
          null
        } else {
          CacheKey(id)
        }
      }

      override fun cacheKeyForObject(
        obj: Map<String, Any?>,
        context: CacheKeyGeneratorContext,
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
  @SingleIn(AppScope::class)
  fun provideCacheKeyResolver(): CacheKeyResolver =
    object : CacheKeyResolver() {
      override fun cacheKeyForField(field: CompiledField, variables: Variables): CacheKey? {
        return null
      }
    }

  @Provides
  @SingleIn(AppScope::class)
  fun provideNormalizedCacheFactory(): NormalizedCacheFactory = MemoryCacheFactory()

  @Provides
  @SingleIn(AppScope::class)
  fun provideHttpEngine(@InternalApi client: Lazy<OkHttpClient>): HttpEngine {
    return DefaultHttpEngine(client::get)
  }

  @Provides
  @SingleIn(AppScope::class)
  fun provideHttpRequestComposer(): HttpRequestComposer {
    return DefaultHttpRequestComposer(SERVER_URL)
  }

  @Provides
  @SingleIn(AppScope::class)
  fun provideNetworkTransport(
    httpEngine: HttpEngine,
    httpRequestComposer: HttpRequestComposer,
  ): NetworkTransport {
    return HttpNetworkTransport.Builder()
      .httpRequestComposer(httpRequestComposer)
      .httpEngine(httpEngine)
      .build()
  }

  @Provides
  @SingleIn(AppScope::class)
  fun provideApolloClient(
    networkTransport: NetworkTransport,
    cacheFactory: NormalizedCacheFactory,
    cacheKeyResolver: CacheKeyResolver,
    cacheKeyGenerator: CacheKeyGenerator,
  ): ApolloClient {
    return ApolloClient.Builder()
      .networkTransport(networkTransport)
      .customScalarAdapters(
        CustomScalarAdapters.Builder()
          .add(DateTime.type, ISO8601InstantApolloAdapter)
          .add(URI.type, HttpUrlApolloAdapter)
          .build()
      )
      .normalizedCache(
        normalizedCacheFactory = cacheFactory,
        cacheResolver = cacheKeyResolver,
        cacheKeyGenerator = cacheKeyGenerator,
      )
      .build()
  }
}
