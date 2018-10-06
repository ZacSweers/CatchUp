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

package io.sweers.catchup.ui.base

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.text.format.DateUtils
import android.view.View
import android.view.View.OnClickListener
import android.view.View.OnLongClickListener
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.text.PrecomputedTextCompat
import androidx.core.view.isVisible
import androidx.core.widget.TextViewCompat
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import io.sweers.catchup.R
import io.sweers.catchup.service.api.BindableCatchUpItemViewHolder
import io.sweers.catchup.service.api.CatchUpItem
import io.sweers.catchup.service.api.Mark
import io.sweers.catchup.util.hide
import io.sweers.catchup.util.kotlin.format
import io.sweers.catchup.util.show
import io.sweers.catchup.util.showIf
import kotterknife.bindView
import org.threeten.bp.Instant

class CatchUpItemViewHolder(itemView: View) : ViewHolder(itemView), BindableCatchUpItemViewHolder {

  internal val container by bindView<ConstraintLayout>(R.id.container)
  internal val tagsContainer by bindView<View>(R.id.tags_container)
  internal val title by bindView<AppCompatTextView>(R.id.title)
  internal val score by bindView<TextView>(R.id.score)
  internal val scoreDivider by bindView<TextView>(R.id.score_divider)
  internal val timestamp by bindView<TextView>(R.id.timestamp)
  internal val author by bindView<TextView>(R.id.author)
  internal val authorDivider by bindView<TextView>(R.id.author_divider)
  internal val source by bindView<TextView>(R.id.source)
  internal val mark by bindView<TextView>(R.id.mark)
  internal val tag by bindView<TextView>(R.id.tag)
  internal val tagDivider by bindView<View>(R.id.tag_divider)

  private val markBackground: Drawable
  private val constraintSet: ConstraintSet

  init {
    markBackground = mark.background
    constraintSet = ConstraintSet()
  }

  override fun itemView(): View = itemView

  override fun tint(@ColorInt color: Int) {
    score.setTextColor(color)
    tag.setTextColor(color)
    scoreDivider.setTextColor(color)
    tintMark(color)
  }

  private fun tintMark(@ColorInt color: Int) {
    if (mark.background == null) {
      mark.background = markBackground.mutate()
    }
    DrawableCompat.setTintList(mark.compoundDrawables[1], ColorStateList.valueOf(color))
  }

  fun setLongClickHandler(longClickHandler: OnLongClickListener?) {
    container.setOnLongClickListener(longClickHandler)
  }

  override fun bind(item: CatchUpItem,
      itemClickHandler: OnClickListener?,
      markClickHandler: OnClickListener?,
      longClickHandler: OnLongClickListener?) {
    title(item.title.trim())
    score(item.score)
    timestamp(item.timestamp)
    author(item.author?.trim())
    source(item.source?.trim())
    tag(item.tag?.trim())

    container.setOnClickListener(itemClickHandler)
    setLongClickHandler(longClickHandler)

    item.mark?.let { sourceMark ->
      mark(sourceMark)
      if (markClickHandler != null) {
        mark.isClickable = true
        mark.isFocusable = true
        mark.setOnClickListener(markClickHandler)
      } else {
        mark.background = null
        mark.isClickable = false
        mark.isFocusable = false
      }
    } ?: run { hideMark() }
  }

  fun title(titleText: CharSequence?) {
    title.setTextFuture(
        PrecomputedTextCompat.getTextFuture(titleText ?: "",
            TextViewCompat.getTextMetricsParams(title), null))
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
    tagsContainer showIf (numVisible > 0)
    when (numVisible) {
      0, 1 -> {
        scoreDivider.hide()
        tagDivider.hide()
      }
      2 -> {
        when {
          timestamp.isVisible -> {
            tagDivider.show()
            scoreDivider.hide()
          }
          score.isVisible -> {
            scoreDivider.show()
            tagDivider.hide()
          }
          else -> {
            tagDivider.show()
            scoreDivider.hide()
          }
        }
      }
      3 -> {
        scoreDivider.show()
        tagDivider.show()
      }
    }
  }

  fun timestamp(instant: Instant?) = timestamp(instant?.toEpochMilli())

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
    with(authorDivider) {
      if (sourceBlank || authorBlank) {
        hide()
      } else {
        show()
      }
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

    constraintSet.apply {
      clone(container)
      // Set the vertical bias on the timestamp view since it is the head of the vertical chain.
      setVerticalBias(R.id.timestamp, getVerticalBias(sourceBlank, authorBlank))
      applyTo(container)
    }
  }

  @SuppressLint("SetTextI18n")
  fun mark(sourceMark: Mark) {
    mark.show()
    sourceMark.text?.let { text ->
      val finalText = if (sourceMark.formatTextAsCount) {
        text.toLong().format()
      } else text
      mark.text = "${sourceMark.textPrefix.orEmpty()}$finalText"
    }

    sourceMark.icon?.let { color ->
      mark.setCompoundDrawablesWithIntrinsicBounds(null,
          AppCompatResources.getDrawable(mark.context, color),
          null,
          null)

      tintMark(sourceMark.iconTintColor ?: score.currentTextColor)
    }

  }

  fun hideMark() = mark.hide()

  private fun getVerticalBias(sourceBlank: Boolean,
      authorBlank: Boolean) = if (sourceBlank && authorBlank) {
    0.5f // Center
  } else if (sourceBlank) {
    0f // Top
  } else {
    0.5f // Center
  }
}
