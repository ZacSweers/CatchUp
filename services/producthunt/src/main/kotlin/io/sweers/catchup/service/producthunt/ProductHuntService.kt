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
package io.sweers.catchup.service.producthunt

import android.content.Context
import androidx.datastore.preferences.preferencesDataStoreFile
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Optional
import com.apollographql.apollo3.api.http.DefaultHttpRequestComposer
import com.apollographql.apollo3.api.http.HttpRequestComposer
import com.apollographql.apollo3.cache.http.HttpFetchPolicy
import com.apollographql.apollo3.cache.http.httpFetchPolicy
import com.apollographql.apollo3.exception.ApolloException
import com.apollographql.apollo3.network.NetworkTransport
import com.apollographql.apollo3.network.http.DefaultHttpEngine
import com.apollographql.apollo3.network.http.HttpEngine
import com.apollographql.apollo3.network.http.HttpNetworkTransport
import com.squareup.anvil.annotations.ContributesMultibinding
import com.squareup.anvil.annotations.ContributesTo
import com.squareup.moshi.Moshi
import dagger.Binds
import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import dev.zacsweers.catchup.auth.AuthInterceptor
import dev.zacsweers.catchup.auth.TokenManager
import dev.zacsweers.catchup.auth.TokenManager.AuthType
import dev.zacsweers.catchup.auth.TokenManager.Credentials
import dev.zacsweers.catchup.auth.TokenStorage
import dev.zacsweers.catchup.di.AppScope
import dev.zacsweers.catchup.di.SingleIn
import io.sweers.catchup.service.api.CatchUpItem
import io.sweers.catchup.service.api.ContentType
import io.sweers.catchup.service.api.DataRequest
import io.sweers.catchup.service.api.DataResult
import io.sweers.catchup.service.api.Mark.Companion.createCommentMark
import io.sweers.catchup.service.api.Service
import io.sweers.catchup.service.api.ServiceKey
import io.sweers.catchup.service.api.ServiceMeta
import io.sweers.catchup.service.api.ServiceMetaKey
import io.sweers.catchup.service.api.TextService
import io.sweers.catchup.service.producthunt.type.DateTime
import io.sweers.catchup.util.apollo.ISO8601InstantApolloAdapter
import io.sweers.catchup.util.injection.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Qualifier
import okhttp3.OkHttpClient
import okio.Path.Companion.toOkioPath

@Qualifier private annotation class InternalApi

private const val SERVICE_KEY = "ph"

@ServiceKey(SERVICE_KEY)
@ContributesMultibinding(AppScope::class, boundType = Service::class)
class ProductHuntService
@Inject
constructor(
  @InternalApi private val serviceMeta: ServiceMeta,
  @InternalApi private val apolloClient: Lazy<ApolloClient>,
) : TextService {

  override fun meta() = serviceMeta

  override suspend fun fetch(request: DataRequest): DataResult {
    val postsQuery =
      apolloClient
        .get()
        .query(
          PostsQuery(
            first = 50,
            after = Optional.presentIfNotNull(request.pageKey.takeUnless { it == "0" })
          )
        )
        .execute()

    if (postsQuery.hasErrors()) {
      throw ApolloException(postsQuery.errors.toString())
    }

    return postsQuery.data
      ?.posts
      ?.edges
      ?.map { it.node }
      .orEmpty()
      .mapIndexed { index, node ->
        with(node) {
          CatchUpItem(
            id = id.toLong(),
            title = name,
            description = tagline,
            score = "▲" to votesCount,
            timestamp = featuredAt,
            author = null, // Always redacted now in their API
            tag = topics.edges.firstOrNull()?.node?.name,
            itemClickUrl = website,
            mark = createCommentMark(count = commentsCount, clickUrl = url),
            indexInResponse = index + request.pageOffset,
            serviceId = meta().id,
            contentType = ContentType.HTML,
          )
        }
      }
      .let {
        val last = postsQuery.data?.posts?.edges?.last()?.node?.id
        DataResult(it, last)
      }
  }
}

private val META =
  ServiceMeta(
    SERVICE_KEY,
    R.string.ph,
    R.color.phAccent,
    R.drawable.logo_ph,
    pagesAreNumeric = true,
    firstPageKey = 0,
    enabled = BuildConfig.PRODUCT_HUNT_CLIENT_ID.run { !isNullOrEmpty() && !equals("null") }
  )

@ContributesTo(AppScope::class)
@Module
abstract class ProductHuntMetaModule {

  @IntoMap
  @ServiceMetaKey(SERVICE_KEY)
  @Binds
  internal abstract fun productHuntServiceMeta(@InternalApi meta: ServiceMeta): ServiceMeta

  companion object {

    @InternalApi @Provides internal fun provideProductHuntServiceMeta(): ServiceMeta = META
  }
}

@ContributesTo(AppScope::class)
@Module(includes = [ProductHuntMetaModule::class])
object ProductHuntModule {

  private const val SERVER_URL = "https://api.producthunt.com/v2/api/graphql"
  private const val AUTH_SERVER = "https://api.producthunt.com"

  @Provides
  @InternalApi
  fun provideProductHuntTokenStorage(@ApplicationContext context: Context): TokenStorage {
    return TokenStorage.create { prefix ->
      context.preferencesDataStoreFile("${prefix}${META.id}").toOkioPath()
    }
  }

  @Provides
  @InternalApi
  fun provideProductHuntTokenManager(
    client: OkHttpClient,
    moshi: Moshi,
    @InternalApi tokenStorage: TokenStorage,
  ): TokenManager {
    return TokenManager.create(
      tokenStorage,
      AUTH_SERVER,
      "/v2/oauth/token",
      client,
      moshi,
      Credentials(
        BuildConfig.PRODUCT_HUNT_CLIENT_ID,
        BuildConfig.PRODUCT_HUNT_CLIENT_SECRET,
        authType = AuthType.JSON,
      )
    )
  }

  @Provides
  @InternalApi
  fun provideProductHuntAuthInterceptor(
    @InternalApi tokenManager: TokenManager,
  ): AuthInterceptor = AuthInterceptor(tokenManager)

  @Provides
  @InternalApi
  fun provideProductHuntOkHttpClient(
    client: OkHttpClient,
    @InternalApi authInterceptor: AuthInterceptor,
  ): OkHttpClient {
    return client.newBuilder().addInterceptor(authInterceptor).build()
  }

  @InternalApi
  @Provides
  @SingleIn(AppScope::class)
  fun provideHttpEngine(@InternalApi client: Lazy<OkHttpClient>): HttpEngine {
    return DefaultHttpEngine { client.get().newCall(it) }
  }

  @InternalApi
  @Provides
  @SingleIn(AppScope::class)
  fun provideHttpRequestComposer(): HttpRequestComposer {
    return DefaultHttpRequestComposer(SERVER_URL)
  }

  @InternalApi
  @Provides
  @SingleIn(AppScope::class)
  fun provideNetworkTransport(
    @InternalApi httpEngine: HttpEngine,
    @InternalApi httpRequestComposer: HttpRequestComposer
  ): NetworkTransport {
    return HttpNetworkTransport.Builder()
      .httpRequestComposer(httpRequestComposer)
      .httpEngine(httpEngine)
      .build()
  }

  @InternalApi
  @Provides
  fun provideProductHuntClient(
    @InternalApi networkTransport: NetworkTransport,
    apolloClient: ApolloClient,
  ): ApolloClient {
    return apolloClient
      .newBuilder()
      .httpFetchPolicy(HttpFetchPolicy.NetworkOnly)
      .addCustomScalarAdapter(DateTime.type, ISO8601InstantApolloAdapter)
      .networkTransport(networkTransport)
      .build()
  }
}


// TODO what about rate limiting errors?
//  {
//   "data": null,
//   "errors": [
//     {
//       "error": "rate_limit_reached",
//       "error_description": "Sorry. You have exceeded the API rate limit, please try again
// later.",
//       "details": {
//         "limit": 25,
//         "remaining": -75,
//         "reset_in": 761
//       }
//     }
//   ]
//  }
