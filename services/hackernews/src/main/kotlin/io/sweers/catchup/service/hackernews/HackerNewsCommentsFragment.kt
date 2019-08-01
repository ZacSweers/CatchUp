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

import android.os.Bundle
import android.text.Html
import android.text.SpannableString
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import io.sweers.catchup.service.api.ScrollableContent
import io.sweers.catchup.service.hackernews.model.HackerNewsComment
import io.sweers.catchup.service.hackernews.model.HackerNewsStory
import io.sweers.catchup.util.d
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.LazyThreadSafetyMode.NONE
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class HackerNewsCommentsFragment @Inject constructor(
  private val database: FirebaseDatabase
) : Fragment(), ScrollableContent {

  companion object {
    const val ARG_DETAIL_KEY = "detailKey"
    const val ARG_DETAIL_TITLE = "detailTitle"
  }

  private val list by lazy(NONE) { view!!.findViewById<RecyclerView>(R.id.list) }
  private val progress by lazy(NONE) { view!!.findViewById<ProgressBar>(R.id.progress) }
  private val toolbar by lazy(NONE) { view!!.findViewById<Toolbar>(R.id.toolbar) }
  private val storyId by lazy(NONE) { arguments!!.getString(ARG_DETAIL_KEY)!! }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    return inflater.inflate(R.layout.hacker_news_story, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    toolbar.title = arguments?.getString(ARG_DETAIL_TITLE) ?: "Untitled"

    viewLifecycleOwner.lifecycleScope.launch {
      val (story, comments) = withContext(Dispatchers.IO) { loadComments() }

      toolbar.title = story.title
      val adapter = CommentsAdapter(comments)
      list.adapter = adapter
      list.layoutManager = LinearLayoutManager(view.context)
      progress.visibility = View.GONE
      list.visibility = View.VISIBLE
    }
  }

  override fun canScrollVertically(directionInt: Int): Boolean {
    return list.canScrollVertically(directionInt)
  }

  private suspend fun loadComments(): Pair<HackerNewsStory, List<HackerNewsComment>> {
    val story = loadStory(storyId)

    return story to story.kids!!.asFlow()
        .map { loadItem(it.toString()) }
        .toList()
  }

  private suspend fun loadStory(id: String) = suspendCancellableCoroutine<HackerNewsStory> { cont ->
    val ref = database.getReference("v0/item/$id")
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
    val ref = database.getReference("v0/item/$id")
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

  private class CommentsAdapter(
    private val comments: List<HackerNewsComment>
  ) : RecyclerView.Adapter<CommentViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
      return CommentViewHolder(TextView(parent.context))
    }

    override fun getItemCount(): Int {
      return comments.size
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
      holder.textView.text = comments[position].text.let { Html.fromHtml(it) } ?: SpannableString("wtf this is null")
    }
  }

  private class CommentViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView)
}
