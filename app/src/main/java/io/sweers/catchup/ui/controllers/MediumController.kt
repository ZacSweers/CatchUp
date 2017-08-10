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
import com.serjltt.moshi.adapters.Wrapped
import com.squareup.moshi.Moshi
import com.uber.autodispose.kotlin.autoDisposeWith
import dagger.Lazy
import dagger.Provides
import dagger.Subcomponent
import dagger.android.AndroidInjector
import io.reactivex.Observable
import io.reactivex.Single
import io.sweers.catchup.BuildConfig
import io.sweers.catchup.R
import io.sweers.catchup.data.EpochInstantJsonAdapter
import io.sweers.catchup.data.InspectorConverterFactory
import io.sweers.catchup.data.LinkManager
import io.sweers.catchup.data.LinkManager.UrlMeta
import io.sweers.catchup.data.RemoteConfigKeys.SMMRY_ENABLED
import io.sweers.catchup.data.medium.MediumService
import io.sweers.catchup.data.medium.model.MediumPost
import io.sweers.catchup.data.medium.model.Post
import io.sweers.catchup.injection.scopes.PerController
import io.sweers.catchup.ui.base.BaseNewsController
import io.sweers.catchup.ui.base.CatchUpItemViewHolder
import okhttp3.OkHttpClient
import org.threeten.bp.Instant
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Qualifier

class MediumController : BaseNewsController<MediumPost> {

  @Inject lateinit var linkManager: LinkManager
  @Inject lateinit var remoteConfig: FirebaseRemoteConfig
  @Inject lateinit var service: MediumService

  constructor() : super()

  constructor(args: Bundle) : super(args)

  override fun onThemeContext(context: Context): Context {
    return ContextThemeWrapper(context, R.style.CatchUp_Medium)
  }

  override fun bindItemView(item: MediumPost, holder: CatchUpItemViewHolder) {
    holder.run {
      title(item.post().title())

      score(Pair(
          "\u2665\uFE0E", // Because lol: https://code.google.com/p/android/issues/detail?id=231068
          item.post()
              .virtuals()
              .recommends()))
      timestamp(item.post()
          .createdAt())

      author(item.user()
          .name())

      tag(item.collection()?.name())

      comments(item.post()
          .virtuals()
          .responsesCreatedCount())
      source(null)

      item.constructUrl().let {
        if (remoteConfig.getBoolean(SMMRY_ENABLED)
            && SmmryController.canSummarize(it)) {
          itemLongClicks()
              .autoDisposeWith(this)
              .subscribe(SmmryController.showFor<Any>(
                  this@MediumController,
                  it,
                  item.post()
                      .title()))
        }
      }

      itemClicks()
          .compose<UrlMeta>(transformUrlToMeta<Any>(item.constructUrl()))
          .flatMapCompletable(linkManager)
          .autoDisposeWith(this)
          .subscribe()
      itemCommentClicks()
          .compose<UrlMeta>(transformUrlToMeta<Any>(item.constructCommentsUrl()))
          .flatMapCompletable(linkManager)
          .autoDisposeWith(this)
          .subscribe()
    }
  }

  override fun getDataSingle(request: BaseNewsController.DataRequest): Single<List<MediumPost>> {
    setMoreDataAvailable(false)
    return service.top()
        .flatMap { references ->
          Observable.fromIterable<Post>(references.post()
              .values)
              .map { post ->
                MediumPost.builder()
                    .post(post)
                    .user(references.user()[post.creatorId()]
                        ?: throw IllegalStateException("Missing user on post!"))
                    .collection(references.collection()[post.homeCollectionId()])
                    .build()
              }
        }
        .toList()
  }

  @PerController
  @Subcomponent(modules = arrayOf(Module::class))
  interface Component : AndroidInjector<MediumController> {

    @Subcomponent.Builder
    abstract class Builder : AndroidInjector.Builder<MediumController>()
  }

  @dagger.Module
  object Module {

    @Qualifier
    private annotation class InternalApi

    @Provides
    @InternalApi
    @JvmStatic
    @PerController
    internal fun provideMediumOkHttpClient(
        client: OkHttpClient): OkHttpClient {
      return client.newBuilder()
          .addInterceptor { chain ->
            var request = chain.request()
            request = request.newBuilder()
                .url(request.url()
                    .newBuilder()
                    .addQueryParameter("format", "json")
                    .build())
                .build()
            val response = chain.proceed(request)
            val source = response.body()!!.source()
            // Medium prefixes with a while loop to prevent javascript eval attacks, so skip to
            // the first open curly brace
            source.skip(source.indexOf('{'.toByte()))
            response
          }
          .build()
    }

    @Provides
    @InternalApi
    @JvmStatic
    @PerController
    internal fun provideMediumMoshi(moshi: Moshi): Moshi {
      return moshi.newBuilder()
          .add(Instant::class.java, EpochInstantJsonAdapter(TimeUnit.MILLISECONDS))
          .add(Wrapped.ADAPTER_FACTORY)
          .build()
    }

    @Provides
    @JvmStatic
    @PerController
    internal fun provideMediumService(@InternalApi client: Lazy<OkHttpClient>,
        @InternalApi moshi: Moshi,
        inspectorConverterFactory: InspectorConverterFactory,
        rxJavaCallAdapterFactory: RxJava2CallAdapterFactory): MediumService {
      val retrofit = Retrofit.Builder().baseUrl(MediumService.ENDPOINT)
          .callFactory { client.get().newCall(it) }
          .addCallAdapterFactory(rxJavaCallAdapterFactory)
          .addConverterFactory(inspectorConverterFactory)
          .addConverterFactory(MoshiConverterFactory.create(moshi))
          .validateEagerly(BuildConfig.DEBUG)
          .build()
      return retrofit.create(MediumService::class.java)
    }
  }
}
