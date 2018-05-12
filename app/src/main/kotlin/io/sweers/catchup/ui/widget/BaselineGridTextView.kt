/*
 * Copyright 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.sweers.catchup.ui.widget

import android.content.Context
import android.content.res.Resources
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.View.MeasureSpec.getMode
import androidx.appcompat.widget.AppCompatTextView
import io.sweers.barber.Barber
import io.sweers.barber.Kind
import io.sweers.barber.StyledAttr
import io.sweers.catchup.R

/**
 * An extension to [android.widget.TextView] which aligns text to a 4dp baseline grid.
 *
 *
 * To achieve this we expose a `lineHeightHint` allowing you to specify the desired line
 * height (alternatively a `lineHeightMultiplierHint` to use a multiplier of the text size).
 * This line height will be adjusted to be a multiple of 4dp to ensure that baselines sit on
 * the grid.
 *
 *
 * We also adjust the `topPadding` to ensure that the first line's baseline is on the grid
 * (relative to the view's top) and the `bottomPadding` to ensure this view's height is a
 * multiple of 4dp so that subsequent views start on the grid.
 */
class BaselineGridTextView @JvmOverloads constructor(context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.textViewStyle)
  : AppCompatTextView(context, attrs, defStyleAttr) {

  companion object {
    private val FOUR_DIP: Float = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
        4f,
        Resources.getSystem().displayMetrics)
  }

  private var extraTopPadding = 0
  private var extraBottomPadding = 0

  var lineHeightMultiplierHint = 1f
    @StyledAttr(R.styleable.BaselineGridTextView_lineHeightMultiplierHint)
    set(value) {
      field = value
      computeLineHeight()
    }

  var lineHeightHint = 0f
    @StyledAttr(value = R.styleable.BaselineGridTextView_lineHeightHint,
        kind = Kind.DIMEN_PIXEL_SIZE)
    set(value) {
      field = value
      computeLineHeight()
    }

  var maxLinesByHeight = false
    @StyledAttr(R.styleable.BaselineGridTextView_maxLinesByHeight)
    set(value) {
      field = value
      requestLayout()
    }


  init {
    Barber.style(this, attrs, R.styleable.BaselineGridTextView, defStyleAttr)
    computeLineHeight()
  }

  override fun getCompoundPaddingTop(): Int {
    // include extra padding to place the first line's baseline on the grid
    return super.getCompoundPaddingTop() + extraTopPadding
  }

  override fun getCompoundPaddingBottom(): Int {
    // include extra padding to make the height a multiple of 4dp
    return super.getCompoundPaddingBottom() + extraBottomPadding
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    extraTopPadding = 0
    extraBottomPadding = 0
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    var height = measuredHeight
    height += ensureBaselineOnGrid()
    height += ensureHeightGridAligned(height)
    setMeasuredDimension(measuredWidth, height)
    checkMaxLines(height, getMode(heightMeasureSpec))
  }

  /**
   * Ensures line height is a multiple of 4dp.
   */
  private fun computeLineHeight() {
    val fm = paint.fontMetricsInt
    val fontHeight = Math.abs(fm.ascent - fm.descent) + fm.leading
    val desiredLineHeight = if (lineHeightHint > 0) lineHeightHint else lineHeightMultiplierHint * fontHeight

    val baselineAlignedLineHeight = (FOUR_DIP * Math.ceil(
        (desiredLineHeight / FOUR_DIP).toDouble()).toFloat()).toInt()
    setLineSpacing((baselineAlignedLineHeight - fontHeight).toFloat(), 1f)
  }

  /**
   * Ensure that the first line of text sits on the 4dp grid.
   */
  private fun ensureBaselineOnGrid(): Int {
    val baseline = baseline.toFloat()
    val gridAlign = baseline % FOUR_DIP
    if (gridAlign != 0f) {
      extraTopPadding = (FOUR_DIP - Math.ceil(gridAlign.toDouble())).toInt()
    }
    return extraTopPadding
  }

  /**
   * Ensure that height is a multiple of 4dp.
   */
  private fun ensureHeightGridAligned(height: Int): Int {
    val gridOverhang = height % FOUR_DIP
    if (gridOverhang != 0f) {
      extraBottomPadding = (FOUR_DIP - Math.ceil(gridOverhang.toDouble())).toInt()
    }
    return extraBottomPadding
  }

  /**
   * When measured with an exact height, text can be vertically clipped mid-line. Prevent
   * this by setting the `maxLines` property based on the available space.
   */
  private fun checkMaxLines(height: Int, heightMode: Int) {
    if (!maxLinesByHeight || heightMode != View.MeasureSpec.EXACTLY) return

    val textHeight = height - compoundPaddingTop - compoundPaddingBottom
    val completeLines = Math.floor((textHeight / lineHeight).toDouble()).toInt()
    maxLines = completeLines
  }
}
