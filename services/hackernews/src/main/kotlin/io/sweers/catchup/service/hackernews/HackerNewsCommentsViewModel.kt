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
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import io.sweers.catchup.service.hackernews.HackerNewsCommentsViewModel.State.Success
import io.sweers.catchup.service.hackernews.model.HackerNewsComment
import io.sweers.catchup.service.hackernews.model.HackerNewsStory
import io.sweers.catchup.service.hackernews.viewmodelbits.ViewModelAssistedFactory
import io.sweers.catchup.util.d
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal class HackerNewsCommentsViewModel @AssistedInject constructor(
  @Assisted private val savedState: SavedStateHandle,
  private val database: FirebaseDatabase
) : ViewModel() {

  sealed class State {
    object Loading : State()
    class Success(val data: Pair<HackerNewsStory, List<HackerNewsComment>>) : State()
    class Failure(val error: Throwable) : State()
  }

  private val storyId = savedState.get<String>(HackerNewsCommentsFragment.ARG_DETAIL_KEY)!!

  val viewState: Flow<State> = ConflatedBroadcastChannel<State>(State.Loading).apply {
    viewModelScope.launch {
      try {
        val result = withContext(Dispatchers.IO) {
          Success(loadComments())
        }
        offer(result)
      } catch (exception: Exception) {
        offer(State.Failure(exception))
      }
    }
  }.asFlow()

  private suspend fun loadComments(): Pair<HackerNewsStory, List<HackerNewsComment>> {
    val story = loadStory(storyId)

    return story to story.kids!!.asFlow()
        .map { loadItem(it.toString()) }
        .toList()
  }

  private suspend fun loadStory(id: String) = suspendCancellableCoroutine<HackerNewsStory> { cont ->
    val ref = database.getReference("v0/item/$id").apply {
      keepSynced(true)
    }
    val listener = object : ValueEventListener {
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

  private suspend fun loadItem(id: String) = suspendCancellableCoroutine<HackerNewsComment> { cont ->
    val ref = database.getReference("v0/item/$id").apply {
      keepSynced(true)
    }
    val listener = object : ValueEventListener {
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

  @AssistedInject.Factory
  interface Factory : ViewModelAssistedFactory<HackerNewsCommentsViewModel>
}
