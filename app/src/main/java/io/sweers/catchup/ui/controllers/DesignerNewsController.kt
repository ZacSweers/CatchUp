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
import com.bluelinelabs.conductor.Controller
import com.serjltt.moshi.adapters.Wrapped
import com.squareup.moshi.Moshi
import com.uber.autodispose.kotlin.autoDisposeWith
import dagger.Binds
import dagger.Lazy
import dagger.Provides
import dagger.Subcomponent
import dagger.android.AndroidInjector
import dagger.multibindings.IntoMap
import io.reactivex.Single
import io.sweers.catchup.BuildConfig
import io.sweers.catchup.R
import io.sweers.catchup.data.ISO8601InstantAdapter
import io.sweers.catchup.data.LinkManager
import io.sweers.catchup.data.LinkManager.UrlMeta
import io.sweers.catchup.data.designernews.DesignerNewsService
import io.sweers.catchup.data.designernews.model.Story
import io.sweers.catchup.data.designernews.model.User
import io.sweers.catchup.injection.ControllerKey
import io.sweers.catchup.ui.base.BaseNewsController
import io.sweers.catchup.ui.base.HasStableId
import okhttp3.OkHttpClient
import org.threeten.bp.Instant
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Inject
import javax.inject.Qualifier

class DesignerNewsController : BaseNewsController<Story> {

  @Inject lateinit var service: DesignerNewsService
  @Inject lateinit var linkManager: LinkManager

  constructor() : super()

  constructor(args: Bundle) : super(args)

  override fun onThemeContext(context: Context): Context {
    return ContextThemeWrapper(context, R.style.CatchUp_DesignerNews)
  }

  override fun bindItemView(item: Story, holder: BaseNewsController.NewsItemViewHolder) {
    //Story story = storyMeta.story;
    //User user = storyMeta.user;
    holder.title(item.title())

    holder.score(Pair.create("▲", item.voteCount()))
    holder.timestamp(item.createdAt())
    //holder.author(user.displayName());
    holder.author(null)

    holder.source(item.hostname())

    holder.comments(item.commentCount())
    holder.tag(item.badge())

    item.url()?.let {
      holder.itemClicks()
          .compose<UrlMeta>(transformUrlToMeta<Any>(it))
          .flatMapCompletable(linkManager)
          .autoDisposeWith(holder)
          .subscribe()
    }
    holder.itemCommentClicks()
        .compose<UrlMeta>(transformUrlToMeta<Any>(item.href()
            .replace("api.", "www.")
            .replace("api/v2/", "")))
        .flatMapCompletable(linkManager)
        .autoDisposeWith(holder)
        .subscribe()
  }

  override fun getDataSingle(request: BaseNewsController.DataRequest): Single<List<Story>> {
    return service.getTopStories(request.page())
    // This won't do for now because /users endpoint sporadically barfs on specific user IDs
    //return service.getTopStories()
    //    .flatMap(stories -> Observable.zip(
    //        Observable.fromIterable(stories),
    //        Observable.fromIterable(stories)
    //            .map(story -> story.links()
    //                .user())
    //            .toList()
    //            .flatMap(ids -> service.getUsers(CommaJoinerList.from(ids)))
    //            .flattenAsObservable(users -> users),
    //        StoryAndUserHolder::new)
    //        .toList());
  }

  @Subcomponent
  interface Component : AndroidInjector<DesignerNewsController> {

    @Subcomponent.Builder
    abstract class Builder : AndroidInjector.Builder<DesignerNewsController>()
  }

  @dagger.Module(subcomponents = arrayOf(Component::class))
  abstract class Module {

    @Qualifier
    private annotation class InternalApi

    @Binds
    @IntoMap
    @ControllerKey(DesignerNewsController::class)
    internal abstract fun bindDesignerNewsControllerInjectorFactory(
        builder: Component.Builder): AndroidInjector.Factory<out Controller>

    @dagger.Module
    companion object {

      @Provides @InternalApi @JvmStatic internal fun provideDesignerNewsMoshi(moshi: Moshi): Moshi {
        return moshi.newBuilder()
            .add(Instant::class.java, ISO8601InstantAdapter())
            .add(Wrapped.ADAPTER_FACTORY)
            .build()
      }

      @Provides @JvmStatic internal fun provideDesignerNewsService(client: Lazy<OkHttpClient>,
          @InternalApi moshi: Moshi,
          rxJavaCallAdapterFactory: RxJava2CallAdapterFactory): DesignerNewsService {
        val retrofit = Retrofit.Builder().baseUrl(DesignerNewsService.ENDPOINT)
            .callFactory { request ->
              client.get()
                  .newCall(request)
            }
            .addCallAdapterFactory(rxJavaCallAdapterFactory)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .validateEagerly(BuildConfig.DEBUG)
            .build()
        return retrofit.create(DesignerNewsService::class.java)
      }
    }
  }

  // TODO Eventually use this when we can safely query User ids
  internal class StoryAndUserHolder(val story: Story, val user: User) : HasStableId {

    override fun stableId(): Long {
      return story.stableId()
    }
  }
}
