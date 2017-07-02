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
import io.reactivex.Observable
import io.reactivex.Single
import io.sweers.catchup.BuildConfig
import io.sweers.catchup.R
import io.sweers.catchup.data.AuthInterceptor
import io.sweers.catchup.data.ISO8601InstantAdapter
import io.sweers.catchup.data.LinkManager
import io.sweers.catchup.data.LinkManager.UrlMeta
import io.sweers.catchup.data.producthunt.ProductHuntService
import io.sweers.catchup.data.producthunt.model.Post
import io.sweers.catchup.injection.ControllerKey
import io.sweers.catchup.ui.base.BaseNewsController
import okhttp3.OkHttpClient
import org.threeten.bp.Instant
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Inject
import javax.inject.Qualifier

class ProductHuntController : BaseNewsController<Post> {

  @Inject lateinit var service: ProductHuntService
  @Inject lateinit var linkManager: LinkManager

  constructor() : super()

  constructor(args: Bundle) : super(args)

  override fun onThemeContext(context: Context): Context {
    return ContextThemeWrapper(context, R.style.CatchUp_ProductHunt)
  }

  override fun bindItemView(item: Post, holder: BaseNewsController.NewsItemViewHolder) {
    holder.run {
      title(item.name())
      score(Pair.create("▲", item.votes_count()))
      timestamp(item.created_at())
      author(item.user()
          .name())
      tag(item.firstTopic)
      source(null)
      comments(item.comments_count())

      itemClicks()
          .compose<UrlMeta>(transformUrlToMeta<Any>(item.redirect_url()))
          .flatMapCompletable(linkManager)
          .autoDisposeWith(this)
          .subscribe()
      itemCommentClicks()
          .compose<UrlMeta>(transformUrlToMeta<Any>(item.discussion_url()))
          .flatMapCompletable(linkManager)
          .autoDisposeWith(this)
          .subscribe()
    }
  }

  override fun getDataSingle(request: BaseNewsController.DataRequest): Single<List<Post>> {
    if (request.multipage) {
      // Backfill pages
      return Observable.range(0, request.page)
          .flatMapSingle { this.getPage(it) }
          .collectInto(mutableListOf<Post>()) { list, collection -> list.addAll(collection) }
          .map { it } // Weird
    } else if (request.fromRefresh) {
      return getPage(request.page)
    } else {
      return getPage(request.page)
    }
  }

  private fun getPage(page: Int): Single<List<Post>> {
    return service.getPosts(page)
  }

  @Subcomponent
  interface Component : AndroidInjector<ProductHuntController> {

    @Subcomponent.Builder
    abstract class Builder : AndroidInjector.Builder<ProductHuntController>()
  }

  @dagger.Module(subcomponents = arrayOf(Component::class))
  abstract class Module {

    @Qualifier
    private annotation class InternalApi

    @Binds
    @IntoMap
    @ControllerKey(ProductHuntController::class)
    internal abstract fun bindProductHuntControllerInjectorFactory(
        builder: Component.Builder): AndroidInjector.Factory<out Controller>

    @dagger.Module
    companion object {

      @Provides @InternalApi @JvmStatic internal fun provideProductHuntOkHttpClient(
          client: OkHttpClient): OkHttpClient {
        return client.newBuilder()
            .addInterceptor(AuthInterceptor.create("Bearer",
                BuildConfig.PROCUCT_HUNT_DEVELOPER_TOKEN))
            .build()
      }

      @Provides @InternalApi @JvmStatic internal fun provideProductHuntMoshi(moshi: Moshi): Moshi {
        return moshi.newBuilder()
            .add(Instant::class.java, ISO8601InstantAdapter())
            .add(Wrapped.ADAPTER_FACTORY)
            .build()
      }

      @Provides @JvmStatic internal fun provideProductHuntService(
          @InternalApi client: Lazy<OkHttpClient>,
          @InternalApi moshi: Moshi,
          rxJavaCallAdapterFactory: RxJava2CallAdapterFactory): ProductHuntService {
        return Retrofit.Builder().baseUrl(ProductHuntService.ENDPOINT)
            .callFactory { client.get().newCall(it) }
            .addCallAdapterFactory(rxJavaCallAdapterFactory)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .validateEagerly(BuildConfig.DEBUG)
            .build()
            .create(ProductHuntService::class.java)
      }
    }
  }
}
