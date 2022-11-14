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
package io.sweers.catchup.service.hackernews

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import catchup.service.hackernews.R
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.squareup.anvil.annotations.ContributesMultibinding
import com.squareup.anvil.annotations.ContributesTo
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.Reusable
import dagger.multibindings.IntoMap
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.core.SingleEmitter
import io.sweers.catchup.service.api.CatchUpItem
import io.sweers.catchup.service.api.DataRequest
import io.sweers.catchup.service.api.DataResult
import io.sweers.catchup.service.api.FragmentKey
import io.sweers.catchup.service.api.Mark.Companion.createCommentMark
import io.sweers.catchup.service.api.Service
import io.sweers.catchup.service.api.ServiceException
import io.sweers.catchup.service.api.ServiceIndex
import io.sweers.catchup.service.api.ServiceKey
import io.sweers.catchup.service.api.ServiceMeta
import io.sweers.catchup.service.api.ServiceMetaIndex
import io.sweers.catchup.service.api.ServiceMetaKey
import io.sweers.catchup.service.api.SummarizationInfo
import io.sweers.catchup.service.api.TextService
import io.sweers.catchup.service.hackernews.model.HackerNewsStory
import io.sweers.catchup.service.hackernews.preview.UrlPreviewModule
import io.sweers.catchup.service.hackernews.viewmodelbits.ViewModelAssistedFactory
import io.sweers.catchup.service.hackernews.viewmodelbits.ViewModelKey
import io.sweers.catchup.util.d
import io.sweers.catchup.util.injection.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Qualifier
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

typealias ViewModelCreator = ViewModelAssistedFactory<out ViewModel>

@Qualifier private annotation class InternalApi

private const val SERVICE_KEY = "hn"

@ServiceKey(SERVICE_KEY)
@ContributesMultibinding(ServiceIndex::class, boundType = Service::class)
class HackerNewsService
@Inject
constructor(
  @InternalApi private val serviceMeta: ServiceMeta,
  private val database: dagger.Lazy<FirebaseDatabase>
) : TextService {

  override fun meta() = serviceMeta

  override fun fetchPage(request: DataRequest): Single<DataResult> {
    val page = request.pageId.toInt()
    val itemsPerPage = 25 // TODO Pref this
    return Single.create { emitter: SingleEmitter<DataSnapshot> ->
        val listener =
          object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
              emitter.onSuccess(dataSnapshot)
            }

            override fun onCancelled(firebaseError: DatabaseError) {
              d { "${firebaseError.code}" }
              emitter.onError(firebaseError.toException())
            }
          }

        val ref = database.get().getReference("v0/topstories").apply { keepSynced(true) }
        emitter.setCancellable { ref.removeEventListener(listener) }
        ref.addValueEventListener(listener)
      }
      .flattenAsObservable { it.children }
      .skip(((page + 1) * itemsPerPage - itemsPerPage).toLong())
      .take(itemsPerPage.toLong())
      .map { d -> d.value as Long }
      .concatMapEager { id ->
        Observable.create<DataSnapshot> { emitter ->
          val ref = database.get().getReference("v0/item/$id")
          val listener =
            object : ValueEventListener {
              override fun onDataChange(dataSnapshot: DataSnapshot) {
                emitter.onNext(dataSnapshot)
                emitter.onComplete()
              }

              override fun onCancelled(firebaseError: DatabaseError) {
                d { "${firebaseError.code}" }
                emitter.onError(firebaseError.toException())
              }
            }
          emitter.setCancellable { ref.removeEventListener(listener) }
          ref.addValueEventListener(listener)
        }
      }
      .filter { it.hasChild("title") } // Some HN items are just empty junk
      .map { HackerNewsStory.create(it) }
      .map {
        val url = it.url
        with(it) {
          CatchUpItem(
            id = id,
            title = title,
            score = "+" to score,
            timestamp = realTime(),
            author = by,
            source = url?.let { it.toHttpUrlOrNull()!!.host },
            tag = realType()?.tag(nullIfStory = true),
            itemClickUrl = url,
            summarizationInfo = SummarizationInfo.from(url),
            mark =
              kids?.size?.let {
                createCommentMark(count = it, clickUrl = "https://news.ycombinator.com/item?id=$id")
              },
            detailKey = id.toString()
          )
        }
      }
      .toList()
      .map { DataResult(it, if (it.isEmpty()) null else (page + 1).toString()) }
      .onErrorResumeNext { t: Throwable ->
        if (t is IllegalArgumentException) {
          // Firebase didn't init
          Single.error(
            ServiceException(
              "Firebase wasn't able to initialize, likely due to missing credentials."
            )
          )
        } else {
          Single.error(t)
        }
      }
  }
}

@ContributesTo(ServiceMetaIndex::class)
@Module
abstract class HackerNewsMetaModule {

  @IntoMap
  @ServiceMetaKey(SERVICE_KEY)
  @Binds
  internal abstract fun hackerNewsServiceMeta(@InternalApi meta: ServiceMeta): ServiceMeta

  companion object {

    @InternalApi
    @Provides
    @Reusable
    internal fun provideHackerNewsServiceMeta(): ServiceMeta =
      ServiceMeta(
        SERVICE_KEY,
        R.string.hn,
        R.color.hnAccent,
        R.drawable.logo_hn,
        pagesAreNumeric = true,
        firstPageKey = "0",
        deeplinkFragment = HackerNewsCommentsFragment::class.java
      )
  }
}

@ContributesTo(ServiceIndex::class)
@Module(
  includes =
    [
      HackerNewsMetaModule::class,
      FragmentViewModelFactoryModule::class,
      ViewModelModule::class,
      UrlPreviewModule::class
    ]
)
abstract class HackerNewsModule {

  @Binds
  @IntoMap
  @FragmentKey(HackerNewsCommentsFragment::class)
  internal abstract fun bindHnFragment(mainFragment: HackerNewsCommentsFragment): Fragment

  companion object {
    @Provides
    internal fun provideDatabase(@ApplicationContext context: Context): FirebaseDatabase {
      val resources = context.resources
      val app =
        FirebaseApp.initializeApp(
          context,
          FirebaseOptions.Builder()
            .setApiKey(resources.getString(R.string.google_api_key))
            .setApplicationId(resources.getString(R.string.google_app_id))
            .setDatabaseUrl("https://hacker-news.firebaseio.com/")
            .setGaTrackingId(resources.getString(R.string.ga_trackingId))
            .setGcmSenderId(resources.getString(R.string.gcm_defaultSenderId))
            .setStorageBucket(resources.getString(R.string.google_storage_bucket))
            .setProjectId(resources.getString(R.string.project_id))
            .build(),
          "HN"
        )
      return FirebaseDatabase.getInstance(app)
    }
  }
}

@Module
internal abstract class ViewModelModule {
  @Binds
  @IntoMap
  @ViewModelKey(HackerNewsCommentsViewModel::class)
  abstract fun mainViewModel(viewModel: HackerNewsCommentsViewModel.Factory): ViewModelCreator
}

// TODO generify this somewhere once something other than HN does it
@Module
object FragmentViewModelFactoryModule {
  @Provides
  fun viewModelFactory(
    viewModels: @JvmSuppressWildcards Map<Class<out ViewModel>, ViewModelCreator>
  ): ViewModelProviderFactoryInstantiator {
    return object : ViewModelProviderFactoryInstantiator {
      override fun create(fragment: Fragment): ViewModelProvider.Factory {
        return object : AbstractSavedStateViewModelFactory(fragment, null) {
          override fun <T : ViewModel> create(
            key: String,
            modelClass: Class<T>,
            arg0: SavedStateHandle // weird name because kapt doesn't preserve the names in the
            // constructor
            ): T {
            // TODO this is ugly extract these constants
            arg0.set("detailKey", fragment.requireArguments().getString("detailKey")!!)
            arg0.set("detailTitle", fragment.requireArguments().getString("detailTitle"))
            @Suppress("UNCHECKED_CAST") return viewModels.getValue(modelClass).create(arg0) as T
          }
        }
      }
    }
  }

  // This exists to avoid the static fragment dependency for dagger. We don't place fragments
  // directly on the DI graph, but rather route them all through a fragment factory that only
  // produces new instances. Instead we defer initialization to the fragment consuming the
  // ViewModelProvider.Factory to pass itself in a deferred fashion.
  //
  // I initially named this ViewModelProviderFactoryFactory, and repent for my sins.
  interface ViewModelProviderFactoryInstantiator {
    fun create(fragment: Fragment): ViewModelProvider.Factory
  }
}
