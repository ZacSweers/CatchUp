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
import com.uber.autodispose.kotlin.autoDisposeWith
import dagger.Lazy
import dagger.Provides
import dagger.Subcomponent
import dagger.android.AndroidInjector
import io.reactivex.Single
import io.sweers.catchup.BuildConfig
import io.sweers.catchup.R
import io.sweers.catchup.data.LinkManager
import io.sweers.catchup.data.LinkManager.UrlMeta
import io.sweers.catchup.data.slashdot.Entry
import io.sweers.catchup.data.slashdot.SlashdotService
import io.sweers.catchup.injection.scopes.PerController
import io.sweers.catchup.ui.base.BaseNewsController
import io.sweers.catchup.util.parsePossiblyOffsetInstant
import io.sweers.catchup.util.unescapeJavaString
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.simplexml.SimpleXmlConverterFactory
import javax.inject.Inject
import javax.inject.Qualifier

class SlashdotController : BaseNewsController<Entry> {

  @Inject lateinit var service: SlashdotService
  @Inject lateinit var linkManager: LinkManager

  constructor() : super()

  constructor(args: Bundle) : super(args)

  override fun onThemeContext(context: Context): Context {
    return ContextThemeWrapper(context, R.style.CatchUp_Slashdot)
  }

  override fun bindItemView(item: Entry, holder: BaseNewsController.NewsItemViewHolder) {
    holder.run {
      title(item.title.unescapeJavaString())

      score(null)
      timestamp(item.updated.parsePossiblyOffsetInstant())
      author(item.author?.name)

      source(item.department)

      comments(item.comments)
      tag(item.section)

      itemClicks()
          .compose<UrlMeta>(transformUrlToMeta<Any>(item.id))
          .flatMapCompletable(linkManager)
          .autoDisposeWith(this)
          .subscribe()
      itemCommentClicks()
          .compose<UrlMeta>(transformUrlToMeta<Any>(item.id + "#comments"))
          .flatMapCompletable(linkManager)
          .autoDisposeWith(this)
          .subscribe()
    }
  }

  override fun getDataSingle(request: BaseNewsController.DataRequest): Single<List<Entry>> {
    setMoreDataAvailable(false)
    return service.main()
        .map { it.itemList }
  }

  @PerController
  @Subcomponent(modules = arrayOf(Module::class))
  interface Component : AndroidInjector<SlashdotController> {

    @Subcomponent.Builder
    abstract class Builder : AndroidInjector.Builder<SlashdotController>()
  }

  @dagger.Module
  object Module {

    @Qualifier
    private annotation class InternalApi

    @Provides
    @InternalApi
    @JvmStatic
    @PerController
    internal fun provideSlashdotOkHttpClient(okHttpClient: OkHttpClient): OkHttpClient {
      return okHttpClient.newBuilder()
          .addNetworkInterceptor { chain ->
            val originalResponse = chain.proceed(chain.request())
            // read from cache for 30 minutes, per slashdot's preferred limit
            val maxAge = 60 * 30
            originalResponse.newBuilder()
                .header("Cache-Control", "public, max-age=" + maxAge)
                .build()
          }
          .build()
    }

    @Provides
    @JvmStatic
    @PerController
    internal fun provideSlashdotService(@InternalApi client: Lazy<OkHttpClient>,
        rxJavaCallAdapterFactory: RxJava2CallAdapterFactory): SlashdotService {
      val retrofit = Retrofit.Builder().baseUrl(SlashdotService.ENDPOINT)
          .callFactory { client.get().newCall(it) }
          .addCallAdapterFactory(rxJavaCallAdapterFactory)
          .addConverterFactory(SimpleXmlConverterFactory.createNonStrict())
          .validateEagerly(BuildConfig.DEBUG)
          .build()
      return retrofit.create(SlashdotService::class.java)
    }
  }
}
