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
package catchup.service.producthunt

import android.content.Context
import androidx.datastore.preferences.preferencesDataStoreFile
import catchup.auth.AuthInterceptor
import catchup.auth.TokenManager
import catchup.auth.TokenManager.AuthType
import catchup.auth.TokenManager.Credentials
import catchup.auth.TokenStorage
import catchup.di.AppScope
import catchup.di.SingleIn
import catchup.service.api.CatchUpItem
import catchup.service.api.Comment
import catchup.service.api.ContentType
import catchup.service.api.DataRequest
import catchup.service.api.DataResult
import catchup.service.api.Detail
import catchup.service.api.Mark.Companion.createCommentMark
import catchup.service.api.Service
import catchup.service.api.ServiceKey
import catchup.service.api.ServiceMeta
import catchup.service.api.ServiceMetaKey
import catchup.service.api.TextService
import catchup.service.producthunt.type.CommentsOrder
import catchup.service.producthunt.type.DateTime
import catchup.util.apollo.ISO8601InstantApolloAdapter
import catchup.util.injection.qualifiers.ApplicationContext
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Optional
import com.apollographql.apollo3.api.http.DefaultHttpRequestComposer
import com.apollographql.apollo3.api.http.HttpRequestComposer
import com.apollographql.apollo3.cache.http.HttpFetchPolicy
import com.apollographql.apollo3.cache.http.httpFetchPolicy
import com.apollographql.apollo3.exception.DefaultApolloException
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
import javax.inject.Inject
import javax.inject.Qualifier
import kotlinx.collections.immutable.toImmutableList
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
            after = Optional.presentIfNotNull(request.pageKey.takeUnless { it == "0" }),
          )
        )
        .execute()

    if (postsQuery.hasErrors()) {
      throw DefaultApolloException(postsQuery.errors.toString())
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
            // TODO alas, product hunt appears to have basically
            //  abandoned their API. https://github.com/producthunt/producthunt-api/issues/292
            // detailKey = id,
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

  override suspend fun fetchDetail(item: CatchUpItem, detailKey: String): Detail {
    val postsQuery =
      apolloClient
        .get()
        .query(
          PostAndCommentsQuery(postId = item.detailKey!!, commentsOrder = CommentsOrder.VOTES_COUNT)
        )
        .execute()

    if (postsQuery.hasErrors()) {
      throw DefaultApolloException(postsQuery.errors.toString())
    }

    val post = postsQuery.data?.post!!
    return Detail.Full(
      id = detailKey,
      itemId = item.id,
      title = post.name,
      text = post.description,
      score = post.votesCount.toLong(),
      commentsCount = post.commentsCount,
      linkUrl = post.website,
      shareUrl = post.url,
      timestamp = post.featuredAt,
      author = null, // Always redacted now in their API
      tag = post.topics.edges.firstOrNull()?.node?.name,
      comments =
        post.comments.edges
          .map {
            val comment = it.node
            Comment(
              id = comment.id,
              serviceId = SERVICE_KEY,
              author = comment.user.username,
              timestamp = comment.createdAt,
              text = comment.body,
              score = comment.votesCount,
              children =
                comment.replies.edges.map {
                  val reply = it.node
                  Comment(
                    id = reply.id,
                    serviceId = SERVICE_KEY,
                    author = reply.user.username,
                    timestamp = reply.createdAt,
                    text = reply.body,
                    score = reply.votesCount,
                    depth = 1,
                    children = emptyList(),
                    clickableUrls = emptyList(),
                  )
                },
              depth = 0,
              clickableUrls = emptyList(),
            )
          }
          .toImmutableList(),
    )
  }
}

private val META =
  ServiceMeta(
    SERVICE_KEY,
    R.string.catchup_service_ph_name,
    R.color.catchup_service_ph_accent,
    R.drawable.catchup_service_ph_logo,
    pagesAreNumeric = true,
    firstPageKey = 0,
    enabled = BuildConfig.PRODUCT_HUNT_CLIENT_ID.run { !isNullOrEmpty() && !equals("null") },
  )

@ContributesTo(AppScope::class)
@Module
abstract class ProductHuntMetaModule {

  @IntoMap
  @ServiceMetaKey(SERVICE_KEY)
  @Binds
  internal abstract fun productHuntServiceMeta(@InternalApi meta: ServiceMeta): ServiceMeta

  companion object {

    @InternalApi @Provides fun provideProductHuntServiceMeta(): ServiceMeta = META
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
      ),
    )
  }

  @Provides
  @InternalApi
  fun provideProductHuntAuthInterceptor(@InternalApi tokenManager: TokenManager): AuthInterceptor =
    AuthInterceptor(tokenManager)

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
    return DefaultHttpEngine(client::get)
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
    @InternalApi httpRequestComposer: HttpRequestComposer,
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
