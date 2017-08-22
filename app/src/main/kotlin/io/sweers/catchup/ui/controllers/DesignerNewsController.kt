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
import com.serjltt.moshi.adapters.Wrapped
import com.squareup.moshi.Moshi
import dagger.Lazy
import dagger.Provides
import dagger.Subcomponent
import dagger.android.AndroidInjector
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import io.sweers.catchup.BuildConfig
import io.sweers.catchup.R
import io.sweers.catchup.data.CatchUpItem
import io.sweers.catchup.data.CatchUpItem2
import io.sweers.catchup.data.ISO8601InstantAdapter
import io.sweers.catchup.data.LinkManager
import io.sweers.catchup.data.designernews.DesignerNewsService
import io.sweers.catchup.data.designernews.model.Story
import io.sweers.catchup.data.designernews.model.User
import io.sweers.catchup.injection.scopes.PerController
import io.sweers.catchup.ui.base.CatchUpItemViewHolder
import io.sweers.catchup.ui.base.StorageBackedNewsController
import io.sweers.catchup.util.collect.toCommaJoinerList
import okhttp3.OkHttpClient
import org.threeten.bp.Instant
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Inject
import javax.inject.Qualifier

class DesignerNewsController : StorageBackedNewsController {

  @Inject lateinit var service: DesignerNewsService
  @Inject lateinit var linkManager: LinkManager

  constructor() : super()

  constructor(args: Bundle) : super(args)

  override fun serviceType() = "dn"

  override fun onThemeContext(context: Context): Context {
    return ContextThemeWrapper(context, R.style.CatchUp_DesignerNews)
  }

  override fun bindItemView(item: CatchUpItem, holder: CatchUpItemViewHolder) {
    holder.bind(this, item, linkManager)
  }

  override fun getDataFromService(page: Int): Single<List<CatchUpItem2>> {
    return service.getTopStories(page)
        .flatMapObservable { stories ->
          Observable.zip(
              // TODO This needs to update to the new /users endpoint behavior, which will only give a best effort result and not necessarily all
              Observable.fromIterable(stories),
              Observable.fromIterable(stories)
                  .map { it.links().user() }
                  .toList()
                  .flatMap { ids -> service.getUsers(ids.toCommaJoinerList()) }
                  .onErrorReturn { (0..stories.size).map { User.NONE } }
                  .flattenAsObservable { it },
              // RxKotlin might help here
              BiFunction<Story, User, StoryAndUserHolder> { story, user ->
                StoryAndUserHolder(story, if (user === User.NONE) null else user)
              })
        }
        .map { (story, user) ->
          with(story) {
            CatchUpItem2(
                id = java.lang.Long.parseLong(id()),
                title = title(),
                score = Pair("▲", voteCount()),
                timestamp = createdAt(),
                author = user?.displayName(),
                source = hostname(),
                commentCount = commentCount(),
                tag = badge(),
                itemClickUrl = url(),
                itemCommentClickUrl = href()
                    .replace("api.", "www.")
                    .replace("api/v2/", "")
            )
          }
        }
        .toList()
  }

  @PerController
  @Subcomponent(modules = arrayOf(Module::class))
  interface Component : AndroidInjector<DesignerNewsController> {

    @Subcomponent.Builder
    abstract class Builder : AndroidInjector.Builder<DesignerNewsController>()
  }

  @dagger.Module
  object Module {

    @Qualifier
    private annotation class InternalApi

    @Provides
    @InternalApi
    @JvmStatic
    @PerController
    internal fun provideDesignerNewsMoshi(moshi: Moshi): Moshi {
      return moshi.newBuilder()
          .add(Instant::class.java, ISO8601InstantAdapter())
          .add(Wrapped.ADAPTER_FACTORY)
          .build()
    }

    @Provides
    @JvmStatic
    @PerController
    internal fun provideDesignerNewsService(client: Lazy<OkHttpClient>,
        @InternalApi moshi: Moshi,
        rxJavaCallAdapterFactory: RxJava2CallAdapterFactory): DesignerNewsService {

      val retrofit = Retrofit.Builder().baseUrl(DesignerNewsService.ENDPOINT)
          .callFactory { client.get().newCall(it) }
          .addCallAdapterFactory(rxJavaCallAdapterFactory)
          .addConverterFactory(MoshiConverterFactory.create(moshi))
          .validateEagerly(BuildConfig.DEBUG)
          .build()
      return retrofit.create(DesignerNewsService::class.java)
    }
  }

  private data class StoryAndUserHolder(val story: Story, val user: User?)
}
