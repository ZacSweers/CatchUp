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
import dagger.Binds
import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
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
import io.sweers.catchup.util.network.AuthInterceptor
import javax.inject.Inject
import javax.inject.Qualifier
import okhttp3.OkHttpClient

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
        .query(PostsQuery(first = 50, after = Optional.presentIfNotNull(request.pageKey)))
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
            score = "▲" to votesCount,
            timestamp = createdAt,
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

@ContributesTo(AppScope::class)
@Module
abstract class ProductHuntMetaModule {

  @IntoMap
  @ServiceMetaKey(SERVICE_KEY)
  @Binds
  internal abstract fun productHuntServiceMeta(@InternalApi meta: ServiceMeta): ServiceMeta

  companion object {

    @InternalApi
    @Provides
    internal fun provideProductHuntServiceMeta(): ServiceMeta =
      ServiceMeta(
        SERVICE_KEY,
        R.string.ph,
        R.color.phAccent,
        R.drawable.logo_ph,
        pagesAreNumeric = true,
        firstPageKey = 0,
        enabled =
          BuildConfig.PRODUCT_HUNT_DEVELOPER_TOKEN.run { !isNullOrEmpty() && !equals("null") }
      )
  }
}

@ContributesTo(AppScope::class)
@Module(includes = [ProductHuntMetaModule::class])
object ProductHuntModule {

  private const val SERVER_URL = "https://api.producthunt.com/v2/api/graphql"

  @Provides
  @InternalApi
  internal fun provideProductHuntOkHttpClient(client: OkHttpClient): OkHttpClient {
    return client
      .newBuilder()
      .addInterceptor(AuthInterceptor("Bearer", BuildConfig.PRODUCT_HUNT_DEVELOPER_TOKEN))
      .build()
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
    httpEngine: HttpEngine,
    httpRequestComposer: HttpRequestComposer
  ): NetworkTransport {
    return HttpNetworkTransport.Builder()
      .httpRequestComposer(httpRequestComposer)
      .httpEngine(httpEngine)
      .build()
  }

  @InternalApi
  @Provides
  internal fun provideProductHuntClient(
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
