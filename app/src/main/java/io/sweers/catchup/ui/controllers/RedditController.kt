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
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.squareup.moshi.Moshi
import com.uber.autodispose.kotlin.autoDisposeWith
import dagger.Lazy
import dagger.Provides
import dagger.Subcomponent
import dagger.android.AndroidInjector
import io.reactivex.Single
import io.sweers.catchup.BuildConfig
import io.sweers.catchup.R
import io.sweers.catchup.data.EpochInstantJsonAdapter
import io.sweers.catchup.data.LinkManager
import io.sweers.catchup.data.LinkManager.UrlMeta
import io.sweers.catchup.data.RemoteConfigKeys.SMMRY_ENABLED
import io.sweers.catchup.data.reddit.RedditService
import io.sweers.catchup.data.reddit.model.RedditLink
import io.sweers.catchup.data.reddit.model.RedditObjectFactory
import io.sweers.catchup.injection.scopes.PerController
import io.sweers.catchup.ui.base.BaseNewsController
import okhttp3.OkHttpClient
import org.threeten.bp.Instant
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Inject
import javax.inject.Qualifier

class RedditController : BaseNewsController<RedditLink> {

  @Inject lateinit var service: RedditService
  @Inject lateinit var linkManager: LinkManager
  @Inject lateinit var remoteConfig: FirebaseRemoteConfig

  private var lastSeen: String? = null

  constructor() : super()

  constructor(args: Bundle) : super(args)

  override fun onThemeContext(context: Context): Context {
    return ContextThemeWrapper(context, R.style.CatchUp_Reddit)
  }

  override fun bindItemView(item: RedditLink, holder: BaseNewsController.NewsItemViewHolder) {
    holder.run {
      title(item.title())

      score(Pair("+", item.score()))
      timestamp(item.createdUtc())
      author("/u/" + item.author())

      source(item.domain() ?: "self")

      comments(item.commentsCount())
      tag(item.subreddit())

      itemClicks()
          .compose<UrlMeta>(transformUrlToMeta<Any>(item.url()))
          .flatMapCompletable(linkManager)
          .autoDisposeWith(this)
          .subscribe()

      val selfText = if (item.isSelf) item.selftext() else null
      if (remoteConfig.getBoolean(SMMRY_ENABLED)
          && SmmryController.canSummarize(item.url(), selfText)) {
        itemLongClicks()
            .autoDisposeWith(this)
            .subscribe(
                SmmryController.showFor<Any>(this@RedditController,
                    item.url(),
                    item.title(),
                    selfText))
      }
      itemCommentClicks()
          .compose<UrlMeta>(transformUrlToMeta<Any>("https://reddit.com/comments/" + item.id()))
          .flatMapCompletable(linkManager)
          .autoDisposeWith(this)
          .subscribe()
    }
  }

  override fun getDataSingle(request: BaseNewsController.DataRequest): Single<List<RedditLink>> {
    return service.frontPage(25, if (request.fromRefresh) null else lastSeen)
        .map { redditListingRedditResponse ->
          lastSeen = redditListingRedditResponse.data().after()
          //noinspection CodeBlock2Expr,unchecked
          @Suppress("UNCHECKED_CAST")
          redditListingRedditResponse.data()
              .children() as List<RedditLink>
        }
  }

  @PerController
  @Subcomponent(modules = arrayOf(Module::class))
  interface Component : AndroidInjector<RedditController> {

    @Subcomponent.Builder
    abstract class Builder : AndroidInjector.Builder<RedditController>()
  }

  @dagger.Module
  object Module {

    @Qualifier
    private annotation class InternalApi

    @InternalApi
    @Provides
    @JvmStatic
    @PerController
    internal fun provideMoshi(upstreamMoshi: Moshi): Moshi {
      return upstreamMoshi.newBuilder()
          .add(RedditObjectFactory.getInstance())
          .add(Instant::class.java, EpochInstantJsonAdapter())
          .build()
    }

    @InternalApi
    @Provides
    @JvmStatic
    @PerController
    internal fun provideRedditOkHttpClient(
        client: OkHttpClient): OkHttpClient {
      return client.newBuilder()
          .addNetworkInterceptor { chain ->
            var request = chain.request()
            val url = request.url()
            request = request.newBuilder()
                .header("User-Agent", "CatchUp app by /u/pandanomic")
                .url(url.newBuilder()
                    .encodedPath(url.encodedPath() + ".json")
                    .build())
                .build()
            chain.proceed(request)
          }
          .build()
    }

    @Provides
    @JvmStatic
    @PerController
    internal fun provideRedditService(@InternalApi client: Lazy<OkHttpClient>,
        rxJavaCallAdapterFactory: RxJava2CallAdapterFactory,
        @InternalApi moshi: Moshi): RedditService {
      val retrofit = Retrofit.Builder().baseUrl(RedditService.ENDPOINT)
          .callFactory { client.get().newCall(it) }
          .addCallAdapterFactory(rxJavaCallAdapterFactory)
          .addConverterFactory(MoshiConverterFactory.create(moshi))
          .validateEagerly(BuildConfig.DEBUG)
          .build()
      return retrofit.create(RedditService::class.java)
    }
  }
}
