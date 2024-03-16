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
package catchup.service.hackernews

import android.content.Context
import catchup.di.AppScope
import catchup.service.api.CatchUpItem
import catchup.service.api.DataRequest
import catchup.service.api.DataResult
import catchup.service.api.Mark.Companion.createCommentMark
import catchup.service.api.Service
import catchup.service.api.ServiceException
import catchup.service.api.ServiceKey
import catchup.service.api.ServiceMeta
import catchup.service.api.ServiceMetaKey
import catchup.service.api.TextService
import catchup.service.hackernews.model.HackerNewsStory
import catchup.util.d
import catchup.util.injection.qualifiers.ApplicationContext
import catchup.util.kotlin.safeOffer
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
import dagger.multibindings.IntoMap
import javax.inject.Inject
import javax.inject.Qualifier
import kotlin.coroutines.resume
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

@Qualifier private annotation class InternalApi

private const val SERVICE_KEY = "hn"

@ServiceKey(SERVICE_KEY)
@ContributesMultibinding(AppScope::class, boundType = Service::class)
class HackerNewsService
@Inject
constructor(
  @InternalApi private val serviceMeta: ServiceMeta,
  private val database: dagger.Lazy<FirebaseDatabase>,
) : TextService {

  override fun meta() = serviceMeta

  @OptIn(ExperimentalCoroutinesApi::class)
  override suspend fun fetch(request: DataRequest): DataResult {
    val page = request.pageKey!!.toInt()
    val itemsPerPage = request.limit
    return callbackFlow {
        val listener =
          object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
              safeOffer(dataSnapshot)
            }

            override fun onCancelled(firebaseError: DatabaseError) {
              d { "${firebaseError.code}" }
              cancel("Firebase error", firebaseError.toException())
            }
          }

        val ref = database.get().getReference("/v0/topstories").apply { keepSynced(true) }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
      }
      .flatMapConcat { it.children.asFlow() }
      .drop((page + 1) * itemsPerPage - itemsPerPage)
      .take(itemsPerPage)
      .map { d -> d.value as Long }
      // TODO concatMapEager?
      .map { id ->
        suspendCancellableCoroutine { cont ->
          val ref = database.get().getReference("/v0/item/$id")
          val listener =
            object : ValueEventListener {
              override fun onDataChange(dataSnapshot: DataSnapshot) {
                cont.resume(dataSnapshot)
              }

              override fun onCancelled(firebaseError: DatabaseError) {
                d { "${firebaseError.code}" }
                cont.cancel(firebaseError.toException())
              }
            }
          cont.invokeOnCancellation { ref.removeEventListener(listener) }
          ref.addValueEventListener(listener)
        }
      }
      .filter { it.hasChild("title") } // Some HN items are just empty junk
      .map { HackerNewsStory.create(it) }
      .catch { t ->
        if (t is IllegalArgumentException) {
          // Firebase didn't init
          throw ServiceException(
            "Firebase wasn't able to initialize, likely due to missing credentials."
          )
        } else {
          throw t
        }
      }
      .toList()
      .mapIndexed { index, it ->
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
            mark =
              kids?.size?.let {
                createCommentMark(count = it, clickUrl = "https://news.ycombinator.com/item?id=$id")
              },
            detailKey = id.toString(),
            indexInResponse = index + request.pageOffset,
            serviceId = meta().id,
          )
        }
      }
      .let { DataResult(it, if (it.isEmpty()) null else (page + 1).toString()) }
  }
}

@ContributesTo(AppScope::class)
@Module
abstract class HackerNewsMetaModule {

  @IntoMap
  @ServiceMetaKey(SERVICE_KEY)
  @Binds
  internal abstract fun hackerNewsServiceMeta(@InternalApi meta: ServiceMeta): ServiceMeta

  companion object {

    @InternalApi
    @Provides
    fun provideHackerNewsServiceMeta(): ServiceMeta =
      ServiceMeta(
        SERVICE_KEY,
        R.string.catchup_service_hn_name,
        R.color.catchup_service_hn_accent,
        R.drawable.catchup_service_hn_logo,
        pagesAreNumeric = true,
        firstPageKey = 0,
        enabled = false, // HN is broken for some reason
      )
  }
}

@ContributesTo(AppScope::class)
@Module
object HackerNewsModule {
  @Provides
  fun provideDatabase(@ApplicationContext context: Context): FirebaseDatabase {
    val app =
      FirebaseApp.getApps(context).firstOrNull()
        ?: run {
          val resources = context.resources
          FirebaseApp.initializeApp(
            context,
            FirebaseOptions.Builder()
              .setApiKey(resources.getString(R.string.google_api_key))
              .setApplicationId(resources.getString(R.string.google_app_id))
              .setDatabaseUrl("https://hacker-news.firebaseio.com")
              .setGaTrackingId(resources.getString(R.string.ga_trackingId))
              .setGcmSenderId(resources.getString(R.string.gcm_defaultSenderId))
              .setStorageBucket(resources.getString(R.string.google_storage_bucket))
              .setProjectId(resources.getString(R.string.project_id))
              .build(),
            "HN",
          )
        }

    return FirebaseDatabase.getInstance(app)
  }
}
