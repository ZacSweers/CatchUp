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

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.squareup.anvil.annotations.ContributesTo
import dagger.Binds
import dagger.Module
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.multibindings.IntoMap
import dev.zacsweers.catchup.di.AppScope
import io.sweers.catchup.base.ui.ViewModelAssistedFactory
import io.sweers.catchup.base.ui.ViewModelKey
import io.sweers.catchup.service.hackernews.HackerNewsCommentsViewModel.State.Success
import io.sweers.catchup.service.hackernews.model.HackerNewsComment
import io.sweers.catchup.service.hackernews.model.HackerNewsStory
import io.sweers.catchup.service.hackernews.preview.UrlPreview
import io.sweers.catchup.service.hackernews.preview.UrlPreviewResponse
import io.sweers.catchup.util.d
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

class HackerNewsCommentsViewModel
@AssistedInject
constructor(
  @Assisted private val savedState: SavedStateHandle,
  private val database: FirebaseDatabase,
  private val urlPreview: UrlPreview
) : ViewModel() {

  sealed class State {
    object Loading : State()
    class Success(
      val data: Pair<HackerNewsStory, List<HackerNewsComment>>,
      val urlPreviewResponse: UrlPreviewResponse
    ) : State()

    class Failure(val error: Throwable) : State()
  }

  private val storyId = savedState.get<String>(HackerNewsCommentsFragment.ARG_DETAIL_KEY)!!

  val viewState: Flow<State> =
    MutableStateFlow<State>(State.Loading).apply {
      viewModelScope.launch {
        value =
          try {
            val story = withContext(Dispatchers.IO) { loadComments() }
            val urlPreview =
              withContext(Dispatchers.IO) { urlPreview.previewUrl("", story.first.url!!) }
            val result = Success(story, urlPreview)
            result
          } catch (exception: Exception) {
            State.Failure(exception)
          }
      }
    }

  private suspend fun loadComments(): Pair<HackerNewsStory, List<HackerNewsComment>> {
    val story = loadStory(storyId)

    return story to story.kids!!.asFlow().map { loadItem(it.toString()) }.toList()
  }

  private suspend fun loadStory(id: String) =
    suspendCancellableCoroutine<HackerNewsStory> { cont ->
      val ref = database.getReference("v0/item/$id").apply { keepSynced(true) }
      val listener =
        object : ValueEventListener {
          override fun onDataChange(dataSnapshot: DataSnapshot) {
            try {
              ref.removeEventListener(this)
              cont.resume(HackerNewsStory.create(dataSnapshot))
            } catch (e: Exception) {
              cont.resumeWithException(e)
            }
          }

          override fun onCancelled(firebaseError: DatabaseError) {
            d { "${firebaseError.code}" }
            cont.resumeWithException(firebaseError.toException())
          }
        }
      cont.invokeOnCancellation { ref.removeEventListener(listener) }
      ref.addValueEventListener(listener)
    }

  private suspend fun loadItem(id: String) =
    suspendCancellableCoroutine<HackerNewsComment> { cont ->
      val ref = database.getReference("v0/item/$id").apply { keepSynced(true) }
      val listener =
        object : ValueEventListener {
          override fun onDataChange(dataSnapshot: DataSnapshot) {
            try {
              ref.removeEventListener(this)
              cont.resume(HackerNewsComment.create(dataSnapshot))
            } catch (e: Exception) {
              cont.resumeWithException(e)
            }
          }

          override fun onCancelled(firebaseError: DatabaseError) {
            d { "${firebaseError.code}" }
            cont.resumeWithException(firebaseError.toException())
          }
        }
      cont.invokeOnCancellation { ref.removeEventListener(listener) }
      ref.addValueEventListener(listener)
    }

  @AssistedFactory interface Factory : ViewModelAssistedFactory<HackerNewsCommentsViewModel>

  @ContributesTo(AppScope::class)
  @Module
  interface FactoryModule {
    @IntoMap
    @Binds
    @ViewModelKey(HackerNewsCommentsViewModel::class)
    fun Factory.bind(): ViewModelAssistedFactory<out ViewModel>
  }
}
