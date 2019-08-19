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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.sweers.catchup.service.api.ScrollableContent
import io.sweers.catchup.service.hackernews.FragmentViewModelFactoryModule.ViewModelProviderFactoryInstantiator
import io.sweers.catchup.service.hackernews.HackerNewsCommentsViewModel.State.Failure
import io.sweers.catchup.service.hackernews.HackerNewsCommentsViewModel.State.Loading
import io.sweers.catchup.service.hackernews.HackerNewsCommentsViewModel.State.Success
import io.sweers.catchup.service.hackernews.model.HackerNewsComment
import io.sweers.catchup.util.hide
import io.sweers.catchup.util.show
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotterknife.bindView
import javax.inject.Inject

internal class HackerNewsCommentsFragment @Inject constructor(
  viewModelFactoryInstantiator: ViewModelProviderFactoryInstantiator
) : Fragment(), ScrollableContent {

  companion object {
    const val ARG_DETAIL_KEY = "detailKey"
    const val ARG_DETAIL_TITLE = "detailTitle"
  }

  private val list by bindView<RecyclerView>(R.id.list)
  private val progress by bindView<ProgressBar>(R.id.progress)
  private val toolbar by bindView<Toolbar>(R.id.toolbar)
  private val viewModel: HackerNewsCommentsViewModel by viewModels { viewModelFactoryInstantiator.create(this) }

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
      viewModel.viewState.collect { state ->
        when (state) {
          is Loading -> {
            progress.show(true)
            list.hide(true)
          }
          is Failure -> {
            toolbar.title = "Failed to load :(. ${state.error.message}"
          }
          is Success -> {
            val (story, comments) = state.data

            toolbar.title = story.title
            val adapter = CommentsAdapter(comments)
            list.adapter = adapter
            list.layoutManager = LinearLayoutManager(view.context)
            progress.hide(true)
            list.show(true)
          }
        }
      }
    }
  }

  override fun canScrollVertically(directionInt: Int): Boolean {
    return list.canScrollVertically(directionInt)
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
      holder.textView.text = try {
        comments[position].text.let {
          @Suppress("DEPRECATION") // I don't know what I'm supposed to replace this with?
          Html.fromHtml(it)
        }
      } catch (e: NullPointerException) {
        "This kills the html: ${comments[position].text}"
      }
    }
  }

  private class CommentViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView)
}
