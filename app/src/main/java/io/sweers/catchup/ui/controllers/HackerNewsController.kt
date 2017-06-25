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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.uber.autodispose.kotlin.autoDisposeWith
import dagger.Binds
import dagger.Lazy
import dagger.Provides
import dagger.Subcomponent
import dagger.android.AndroidInjector
import dagger.multibindings.IntoMap
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.SingleEmitter
import io.sweers.catchup.R
import io.sweers.catchup.data.LinkManager
import io.sweers.catchup.data.LinkManager.UrlMeta
import io.sweers.catchup.data.RemoteConfigKeys.SMMRY_ENABLED
import io.sweers.catchup.data.hackernews.model.HackerNewsStory
import io.sweers.catchup.injection.ControllerKey
import io.sweers.catchup.ui.base.BaseNewsController
import okhttp3.HttpUrl
import timber.log.Timber
import javax.inject.Inject

class HackerNewsController : BaseNewsController<HackerNewsStory> {

  @Inject lateinit var linkManager: LinkManager
  @Inject lateinit var remoteConfig: FirebaseRemoteConfig
  @Inject lateinit var database: Lazy<FirebaseDatabase>

  constructor() : super()

  constructor(args: Bundle) : super(args)

  override fun onThemeContext(context: Context): Context {
    return ContextThemeWrapper(context, R.style.CatchUp_HackerNews)
  }

  override fun bindItemView(item: HackerNewsStory, holder: BaseNewsController.NewsItemViewHolder) {
    holder.run {
      title(item.title())
      score(Pair.create("+", item.score()))
      timestamp(item.time())
      author(item.by())

      val url = item.url()
      source(url?.let { HttpUrl.parse(it)!!.host() })

      // TODO Adapter to coerce this to Collections.emptyList()?
      val commentsCount = item.kids()?.size ?: 0
      comments(commentsCount)
      tag(null)

      if (!url.isNullOrBlank()) {
        if (remoteConfig.getBoolean(SMMRY_ENABLED)) {
          itemLongClicks()
              .autoDisposeWith(this)
              .subscribe(SmmryController.showFor<Any>(this@HackerNewsController, url, item.title()))
        }

        itemClicks()
            .compose<UrlMeta>(transformUrlToMeta<Any>(url))
            .flatMapCompletable(linkManager)
            .autoDisposeWith(this)
            .subscribe()

        itemCommentClicks()
            .compose<UrlMeta>(
                transformUrlToMeta<Any>("https://news.ycombinator.com/item?id=" + item.id()))
            .flatMapCompletable(linkManager)
            .autoDisposeWith(this)
            .subscribe()
      }
    }
  }

  override fun getDataSingle(
      request: BaseNewsController.DataRequest): Single<List<HackerNewsStory>> {
    val itemsPerPage = 25 // TODO Pref this
    return Single.create { emitter: SingleEmitter<DataSnapshot> ->
      val listener = object : ValueEventListener {
        override fun onDataChange(dataSnapshot: DataSnapshot) {
          emitter.onSuccess(dataSnapshot)
        }

        override fun onCancelled(firebaseError: DatabaseError) {
          Timber.d("%d", firebaseError.code)
          emitter.onError(firebaseError.toException())
        }
      }

      val ref = database.get()
          .getReference("v0/topstories")
      emitter.setCancellable { ref.removeEventListener(listener) }
      ref.addValueEventListener(listener)
    }
        .flattenAsObservable { it.children }
        .skip(((request.page() + 1) * itemsPerPage - itemsPerPage).toLong())
        .take(itemsPerPage.toLong())
        .map { d -> d.value as Long }
        .concatMapEager { id ->
          Observable.create<DataSnapshot> { emitter ->
            val ref = database.get()
                .getReference("v0/item/" + id)
            val listener = object : ValueEventListener {
              override fun onDataChange(dataSnapshot: DataSnapshot) {
                emitter.onNext(dataSnapshot)
                emitter.onComplete()
              }

              override fun onCancelled(firebaseError: DatabaseError) {
                Timber.d("%d", firebaseError.code)
                emitter.onError(firebaseError.toException())
              }
            }
            emitter.setCancellable { ref.removeEventListener(listener) }
            ref.addValueEventListener(listener)
          }
        }
        .map { HackerNewsStory.create(it) }
        .toList()
  }

  @Subcomponent
  interface Component : AndroidInjector<HackerNewsController> {

    @Subcomponent.Builder
    abstract class Builder : AndroidInjector.Builder<HackerNewsController>()
  }

  @dagger.Module(subcomponents = arrayOf(Component::class))
  abstract class Module {

    @Binds
    @IntoMap
    @ControllerKey(HackerNewsController::class)
    internal abstract fun bindHackerNewsControllerInjectorFactory(
        builder: Component.Builder): AndroidInjector.Factory<out Controller>

    @dagger.Module
    companion object {

      @Provides @JvmStatic internal fun provideDataBase(): FirebaseDatabase {
        return FirebaseDatabase.getInstance("https://hacker-news.firebaseio.com/")
      }
    }
  }
}
