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
import io.sweers.catchup.util.isVisible
import io.sweers.catchup.util.makeGone
import io.sweers.catchup.util.makeVisible
import org.threeten.bp.Instant

class CatchUpItemViewHolder(itemView: View) : RxViewHolder(itemView) {

  companion object {
    val COMPLETABLE_FUNC = Function<UrlMeta, Completable> { Completable.complete() }
  }

  @BindView(R.id.container) lateinit var container: View
  @BindView(R.id.tags_container) lateinit var tagsContainer: View
  @BindView(R.id.title) lateinit var title: TextView
  @BindView(R.id.score) lateinit var score: TextView
  @BindView(R.id.score_divider) lateinit var scoreDivider: TextView
  @BindView(R.id.timestamp) lateinit var timestamp: TextView
  @BindView(R.id.author) lateinit var author: TextView
  @BindView(R.id.author_divider) lateinit var authorDivider: TextView
  @BindView(R.id.source) lateinit var source: TextView
  @BindView(R.id.comments) lateinit var comments: TextView
  @BindView(R.id.tag) lateinit var tag: TextView
  @BindView(R.id.tag_divider) lateinit var tagDivider: View
  private var unbinder: Unbinder? = null

  init {
    unbinder?.unbind()
    unbinder = ButterKnife.bind(this, itemView)
  }

  fun bind(controller: ServiceController, item: CatchUpItem, linkManager: LinkManager? = null) {
    title(item.title)
    score(item.score)
    timestamp(item.timestamp)
    author(item.author)
    source(item.source)
    comments(item.commentCount)
    tag(item.tag)

    val itemClickUrl = item.itemClickUrl ?: item.itemCommentClickUrl
    itemClickUrl?.let {
      itemClicks()
          .compose<UrlMeta>(controller.transformUrlToMeta<Any>(it))
          .flatMapCompletable(linkManager ?: COMPLETABLE_FUNC)
          .autoDisposeWith(this)
          .subscribe()
    }
    item.itemCommentClickUrl?.let {
      itemCommentClicks()
          .compose<UrlMeta>(controller.transformUrlToMeta<Any>(it))
          .flatMapCompletable(linkManager ?: COMPLETABLE_FUNC)
          .autoDisposeWith(this)
          .subscribe()
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
    if (scoreValue == null) {
      score.makeGone()
    } else {
      score.makeVisible()
      score.text = String.format("%s %s",
          scoreValue.first,
          scoreValue.second.toLong().format())
    }
    updateDividerVisibility()
  }

  fun tag(text: String?) {
    if (text == null) {
      tag.makeGone()
    } else {
      tag.makeVisible()
      tag.text = text.capitalize()
    }
    updateDividerVisibility()
  }

  private fun updateDividerVisibility() {
    val numVisible = arrayOf(score, tag, timestamp)
        .asSequence()
        .count { it.isVisible() }
    when (numVisible) {
      0, 1 -> {
        scoreDivider.makeGone()
        tagDivider.makeGone()
        tagsContainer.makeGone()
      }
      2 -> {
        tagsContainer.makeVisible()
        if (score.isVisible()) {
          scoreDivider.makeVisible()
          tagDivider.makeGone()
        } else {
          tagDivider.makeVisible()
          scoreDivider.makeGone()
        }
      }
      3 -> {
        tagsContainer.makeVisible()
        scoreDivider.makeVisible()
        tagDivider.makeVisible()
      }
    }
  }

  fun timestamp(instant: Instant?) {
    timestamp(instant?.toEpochMilli())
  }

  private fun timestamp(date: Long?) {
    if (date != null) {
      with(timestamp) {
        makeVisible()
        text = DateUtils.getRelativeTimeSpanString(date,
            System.currentTimeMillis(),
            0L,
            DateUtils.FORMAT_ABBREV_ALL)
      }
    } else {
      timestamp.makeGone()
    }
    updateDividerVisibility()
  }

  fun author(authorText: CharSequence?) {
    if (authorText == null) {
      author.makeGone()
      authorDivider.makeGone()
    } else {
      authorDivider.makeVisible()
      author.makeVisible()
      author.text = authorText
    }
  }

  fun source(sourceText: CharSequence?) {
    if (sourceText == null) {
      source.makeGone()
      authorDivider.makeGone()
    } else {
      if (author.isVisible()) {
        authorDivider.makeVisible()
      }
      source.makeVisible()
      source.text = sourceText
    }
  }

  fun comments(commentsCount: Int) {
    comments.makeVisible()
    comments.text = commentsCount.toLong().format()
  }

  fun hideComments() {
    comments.makeGone()
  }
}
