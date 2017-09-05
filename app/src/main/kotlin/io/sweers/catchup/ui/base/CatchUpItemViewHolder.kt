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

package io.sweers.catchup.ui.base

import android.support.v7.widget.RxViewHolder
import android.text.format.DateUtils
import android.view.View
import android.widget.TextView
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.Unbinder
import com.jakewharton.rxbinding2.view.RxView
import com.uber.autodispose.kotlin.autoDisposeWith
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.functions.Function
import io.sweers.catchup.R
import io.sweers.catchup.data.CatchUpItem
import io.sweers.catchup.data.LinkManager
import io.sweers.catchup.data.LinkManager.UrlMeta
import io.sweers.catchup.util.format
import io.sweers.catchup.util.hide
import io.sweers.catchup.util.isVisible
import io.sweers.catchup.util.show
import org.threeten.bp.Instant

class CatchUpItemViewHolder(itemView: View) : RxViewHolder(itemView) {

  companion object {
    val COMPLETABLE_FUNC = Function<UrlMeta, Completable> { Completable.complete() }
  }

  @BindView(R.id.container) internal lateinit var container: View
  @BindView(R.id.tags_container) internal lateinit var tagsContainer: View
  @BindView(R.id.title) internal lateinit var title: TextView
  @BindView(R.id.score) internal lateinit var score: TextView
  @BindView(R.id.score_divider) internal lateinit var scoreDivider: TextView
  @BindView(R.id.timestamp) internal lateinit var timestamp: TextView
  @BindView(R.id.author) internal lateinit var author: TextView
  @BindView(R.id.author_divider) internal lateinit var authorDivider: TextView
  @BindView(R.id.source) internal lateinit var source: TextView
  @BindView(R.id.comments) internal lateinit var comments: TextView
  @BindView(R.id.tag) internal lateinit var tag: TextView
  @BindView(R.id.tag_divider) internal lateinit var tagDivider: View
  private var unbinder: Unbinder? = null

  init {
    unbinder?.unbind()
    unbinder = ButterKnife.bind(this, itemView)
  }

  fun bind(controller: ServiceController,
      item: CatchUpItem,
      linkManager: LinkManager? = null,
      itemClickHandler: ((String) -> Unit)? = null,
      commentClickHandler: ((String) -> Unit)? = null) {
    title(item.title)
    score(item.score)
    timestamp(item.timestamp)
    author(item.author)
    source(item.source)
    comments(item.commentCount)
    tag(item.tag)

    val itemClickUrl = item.itemClickUrl ?: item.itemCommentClickUrl
    itemClickUrl?.let {
      if (itemClickHandler != null) {
        itemClickHandler(it)
      } else {
        itemClicks()
            .compose<UrlMeta>(controller.transformUrlToMeta<Any>(it))
            .flatMapCompletable(linkManager ?: COMPLETABLE_FUNC)
            .autoDisposeWith(this)
            .subscribe()
      }
    }
    item.itemCommentClickUrl?.let {
      if (commentClickHandler != null) {
        commentClickHandler(it)
      } else {
        itemCommentClicks()
            .compose<UrlMeta>(controller.transformUrlToMeta<Any>(it))
            .flatMapCompletable(linkManager ?: COMPLETABLE_FUNC)
            .autoDisposeWith(this)
            .subscribe()
      }
    } ?: hideComments()

    if (item.hideComments) {
      hideComments()
    }
  }

  fun itemClicks(): Observable<Any> {
    return RxView.clicks(container)
  }

  fun itemLongClicks(): Observable<Any> {
    return RxView.longClicks(container)
  }

  fun itemCommentClicks(): Observable<Any> {
    return RxView.clicks(comments)
  }

  fun title(titleText: CharSequence?) {
    title.text = titleText
  }

  fun score(scoreValue: Pair<String, Int>?) {
    score.text = scoreValue?.let {
      String.format("%s %s",
          scoreValue.first,
          scoreValue.second.toLong().format())
    }
    updateDividerVisibility()
  }

  fun tag(text: String?) {
    tag.text = text?.capitalize()
    updateDividerVisibility()
  }

  private fun updateDividerVisibility() {
    val numVisible = arrayOf(score, tag, timestamp)
        .asSequence()
        .map { (it to it.text.isBlank()) }
        .onEach { (view, isBlank) ->
          if (isBlank) {
            view.hide()
          } else {
            view.show()
          }
        }
        .count { (_, isBlank) -> !isBlank }
    when (numVisible) {
      0, 1 -> {
        scoreDivider.hide()
        tagDivider.hide()
        if (numVisible == 0) {
          tagsContainer.hide()
        } else {
          tagsContainer.show()
        }
      }
      2 -> {
        tagsContainer.show()
        if (score.isVisible()) {
          scoreDivider.show()
          tagDivider.hide()
        } else {
          tagDivider.show()
          scoreDivider.hide()
        }
      }
      3 -> {
        tagsContainer.show()
        scoreDivider.show()
        tagDivider.show()
      }
    }
  }

  fun timestamp(instant: Instant?) {
    timestamp(instant?.toEpochMilli())
  }

  private fun timestamp(date: Long?) {
    timestamp.text = date?.let {
      DateUtils.getRelativeTimeSpanString(it,
          System.currentTimeMillis(),
          0L,
          DateUtils.FORMAT_ABBREV_ALL)
    }
    updateDividerVisibility()
  }

  fun author(authorText: CharSequence?) {
    author.text = authorText
    updateAttributionVisibility()
  }

  fun source(sourceText: CharSequence?) {
    source.text = sourceText
    updateAttributionVisibility()
  }

  private fun updateAttributionVisibility() {
    val sourceBlank = source.text.isBlank()
    val authorBlank = author.text.isBlank()
    if (sourceBlank || authorBlank) {
      authorDivider.hide()
    }
    if (sourceBlank) {
      source.hide()
    } else {
      source.show()
    }
    if (authorBlank) {
      author.hide()
    } else {
      author.show()
    }
  }

  fun comments(commentsCount: Int) {
    comments.show()
    comments.text = commentsCount.toLong().format()
  }

  fun hideComments() {
    comments.hide()
  }
}
