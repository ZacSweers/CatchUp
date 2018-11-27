/*
 * Copyright (c) 2018 Zac Sweers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.sweers.catchup.service.hackernews

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.Reusable
import dagger.multibindings.IntoMap
import io.sweers.catchup.service.api.CatchUpItem
import io.sweers.catchup.service.api.DataRequest
import io.sweers.catchup.service.api.DataResult
import io.sweers.catchup.service.api.LinkHandler
import io.sweers.catchup.service.api.Mark.Companion.createCommentMark
import io.sweers.catchup.service.api.Service
import io.sweers.catchup.service.api.ServiceException
import io.sweers.catchup.service.api.ServiceKey
import io.sweers.catchup.service.api.ServiceMeta
import io.sweers.catchup.service.api.ServiceMetaKey
import io.sweers.catchup.service.api.SummarizationInfo
import io.sweers.catchup.service.api.TextService
import io.sweers.catchup.service.hackernews.model.HackerNewsStory
import io.sweers.catchup.serviceregistry.annotations.Meta
import io.sweers.catchup.serviceregistry.annotations.ServiceModule
import io.sweers.catchup.util.d
import io.sweers.catchup.util.kotlin.concatMapEager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import javax.inject.Inject
import javax.inject.Qualifier
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Qualifier
private annotation class InternalApi

private const val SERVICE_KEY = "hn"

internal class HackerNewsService @Inject constructor(
    @InternalApi private val serviceMeta: ServiceMeta,
    private val database: dagger.Lazy<FirebaseDatabase>,
    private val linkHandler: LinkHandler)
  : TextService {

  override fun meta() = serviceMeta

  // Copied from https://github.com/apollographql/apollo-android/issues/606#issuecomment-354562134
  private suspend fun listen(
      refResolver: () -> DatabaseReference) = suspendCancellableCoroutine<DataSnapshot> { cont ->
    val ref = refResolver()
    val listener = object : ValueEventListener {
      override fun onDataChange(dataSnapshot: DataSnapshot) {
        cont.resume(dataSnapshot)
        ref.removeEventListener(this)
      }

      override fun onCancelled(firebaseError: DatabaseError) {
        d { "${firebaseError.code}" }
        cont.resumeWithException(firebaseError.toException())
      }
    }

    cont.invokeOnCancellation { ref.removeEventListener(listener) }
    ref.addValueEventListener(listener)
  }

  override suspend fun fetchPage(request: DataRequest) = withContext(Dispatchers.Default) {
    val page = request.pageId.toInt()
    val itemsPerPage = 25 // TODO Pref this
    try {
      listen { database.get().getReference("v0/topstories") }
          .children
          .asSequence()
          .drop((page + 1) * itemsPerPage - itemsPerPage)
          .take(itemsPerPage)
          .map { d -> d.value as Long }
          .concatMapEager { id ->
            listen { database.get().getReference("v0/item/$id") }
          }
          .asSequence()
          .filter { it.hasChild("title") }  // Some HN items are just empty junk
          .map(HackerNewsStory::create)
          .concatMapEager { story ->
            val url = story.url
            with(story) {
              CatchUpItem(
                  id = id,
                  title = title,
                  score = "+" to score,
                  timestamp = realTime(),
                  author = by,
                  source = url?.let { HttpUrl.parse(it)!!.host() },
                  tag = realType()?.tag(nullIfStory = true),
                  itemClickUrl = url,
                  summarizationInfo = SummarizationInfo.from(url),
                  mark = kids?.size?.let {
                    createCommentMark(count = it,
                        clickUrl = "https://news.ycombinator.com/item?id=$id")
                  }
              )
            }
          }
          .let { DataResult(it, if (it.isEmpty()) null else (page + 1).toString()) }
    } catch (t: Throwable) {
      if (BuildConfig.DEBUG && t is IllegalArgumentException) {
        // Firebase didn't init
        throw ServiceException(
            "Firebase wasn't able to initialize, likely due to missing credentials.")
      } else {
        throw t
      }
    }
  }

  override fun linkHandler() = linkHandler
}

@Meta
@ServiceModule
@Module
abstract class HackerNewsMetaModule {

  @IntoMap
  @ServiceMetaKey(SERVICE_KEY)
  @Binds
  internal abstract fun hackerNewsServiceMeta(@InternalApi meta: ServiceMeta): ServiceMeta

  @Module
  companion object {

    @InternalApi
    @Provides
    @Reusable
    @JvmStatic
    internal fun provideHackerNewsServiceMeta() = ServiceMeta(
        SERVICE_KEY,
        R.string.hn,
        R.color.hnAccent,
        R.drawable.logo_hn,
        pagesAreNumeric = true,
        firstPageKey = "0"
    )
  }
}

@ServiceModule
@Module(includes = [HackerNewsMetaModule::class])
abstract class HackerNewsModule {

  @IntoMap
  @ServiceKey(SERVICE_KEY)
  @Binds
  internal abstract fun hackerNewsService(hackerNewsService: HackerNewsService): Service

  @Module
  companion object {

    @Provides
    @JvmStatic
    internal fun provideDataBase() =
        FirebaseDatabase.getInstance("https://hacker-news.firebaseio.com/")
  }
}
