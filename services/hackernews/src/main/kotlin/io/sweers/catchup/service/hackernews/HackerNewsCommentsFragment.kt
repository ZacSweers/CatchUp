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

import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Html
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.api.load
import coil.size.Precision
import coil.size.Scale
import coil.size.ViewSizeResolver
import io.sweers.catchup.base.ui.generateAsync
import io.sweers.catchup.service.api.ScrollableContent
import io.sweers.catchup.service.hackernews.FragmentViewModelFactoryModule.ViewModelProviderFactoryInstantiator
import io.sweers.catchup.service.hackernews.HackerNewsCommentsViewModel.State.Failure
import io.sweers.catchup.service.hackernews.HackerNewsCommentsViewModel.State.Loading
import io.sweers.catchup.service.hackernews.HackerNewsCommentsViewModel.State.Success
import io.sweers.catchup.service.hackernews.databinding.HackerNewsStoryBinding
import io.sweers.catchup.service.hackernews.databinding.StoryItemBinding
import io.sweers.catchup.service.hackernews.model.HackerNewsComment
import io.sweers.catchup.util.hide
import io.sweers.catchup.util.show
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrl
import javax.inject.Inject

internal class HackerNewsCommentsFragment @Inject constructor(
    viewModelFactoryInstantiator: ViewModelProviderFactoryInstantiator
) : Fragment(), ScrollableContent {

  companion object {
    const val ARG_DETAIL_KEY = "detailKey"
    const val ARG_DETAIL_TITLE = "detailTitle"
  }

  private lateinit var binding: HackerNewsStoryBinding
  private val list get() = binding.list
  private val progress get() = binding.progress

  private val viewModel: HackerNewsCommentsViewModel by viewModels {
    viewModelFactoryInstantiator.create(this)
  }

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View? {
    binding = HackerNewsStoryBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    binding.link.text = arguments?.getString(ARG_DETAIL_TITLE) ?: "Untitled"

    viewLifecycleOwner.lifecycleScope.launch {
      viewModel.viewState.collect { state ->
        when (state) {
          is Loading -> {
            progress.show(true)
            list.hide(true)
          }
          is Failure -> {
            binding.link.text = "Failed to load :(. ${state.error.message}"
          }
          is Success -> {
            val (story, comments) = state.data

            // Load the preview container
            binding.urlImage.load(state.urlPreviewResponse.image) {
              precision(Precision.EXACT)
              size(ViewSizeResolver(binding.urlImage))
              scale(Scale.FILL)
              crossfade(true)
              listener(
                  onSuccess = { data, source ->
                    val bitmap = (binding.urlImage.drawable as BitmapDrawable).bitmap
                    viewLifecycleOwner.lifecycleScope.launch paletteLaunch@{
                      val swatch = Palette.from(bitmap)
                          .clearFilters()
                          .generateAsync()
                          ?.dominantSwatch
                          ?: return@paletteLaunch
                      binding.urlTextContainer.background = ColorDrawable(swatch.rgb)
                      binding.urlTitle.setTextColor(swatch.titleTextColor)
                      binding.urlUrl.setTextColor(swatch.bodyTextColor)
                    }
                  }
              )
            }
            binding.urlTextContainer.setOnClickListener {
              // state.urlPreviewResponse.url
            }
            binding.urlTitle.text = state.urlPreviewResponse.title
            binding.urlUrl.text = state.urlPreviewResponse.url.toHttpUrl().host

            binding.score.text = story.score.toString()
            binding.link.text = story.title
            binding.author.text = story.by
            binding.time.text = DateUtils.getRelativeTimeSpanString(story.realTime().toEpochMilli(),
                System.currentTimeMillis(),
                0L,
                DateUtils.FORMAT_ABBREV_ALL
            )
            val adapter = CommentsAdapter(comments)
            list.adapter = adapter
            val layoutManager = LinearLayoutManager(view.context)
            list.layoutManager = layoutManager
            list.addItemDecoration(DividerItemDecoration(list.context, layoutManager.orientation))
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
      return CommentViewHolder(
          StoryItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun getItemCount(): Int {
      return comments.size
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
      val comment = comments[position]
      holder.binding.depth.visibility = View.GONE
      holder.binding.commentAuthor.text = comment.by
      holder.binding.commentTimeAgo.text = DateUtils.getRelativeTimeSpanString(
          comment.realTime().toEpochMilli(),
          System.currentTimeMillis(),
          0L,
          DateUtils.FORMAT_ABBREV_ALL
      )
      holder.binding.commentText.text = try {
        comment.text.let {
          @Suppress("DEPRECATION") // I don't know what I'm supposed to replace this with?
          Html.fromHtml(it).trimEnd()
        }
      } catch (e: NullPointerException) {
        "This kills the html: ${comment.text}"
      }
    }
  }

  private class CommentViewHolder(
      val binding: StoryItemBinding
  ) : RecyclerView.ViewHolder(binding.root)
}
