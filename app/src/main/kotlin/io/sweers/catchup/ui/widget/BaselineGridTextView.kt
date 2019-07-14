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
package io.sweers.catchup.ui.widget

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.util.TypedValue
import androidx.annotation.FontRes
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.res.use
import io.sweers.catchup.R
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor

/**
 * An extension to [AppCompatTextView] which aligns text to a 4dp baseline grid.
 *
 * To achieve this we expose a `lineHeightHint` allowing you to specify the desired line
 * height (alternatively a `lineHeightMultiplierHint` to use a multiplier of the text size).
 * This line height will be adjusted to be a multiple of 4dp to ensure that baselines sit on
 * the grid.
 *
 * We also adjust spacing above and below the text to ensure that the first line's baseline sits on
 * the grid (relative to the view's top) & that this view's height is a multiple of 4dp so that
 * subsequent views start on the grid.
 */
class BaselineGridTextView @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null,
  defStyleAttr: Int = android.R.attr.textViewStyle
) : AppCompatTextView(context, attrs,
    defStyleAttr) {

  private val FOUR_DIP: Float

  var lineHeightMultiplierHint = 1f
    set(value) {
      field = value
      computeLineHeight()
    }
  var lineHeightHint = 0f
    set(value) {
      field = value
      computeLineHeight()
    }
  var maxLinesByHeight = false
    set(value) {
      field = value
      requestLayout()
    }
  private var extraTopPadding = 0
  private var extraBottomPadding = 0

  @FontRes
  var fontResId = 0
    private set

  init {
    context.obtainStyledAttributes(
        attrs, R.styleable.BaselineGridTextView, defStyleAttr, 0).use {
      // first check TextAppearance for line height & font attributes
      if (it.hasValue(R.styleable.BaselineGridTextView_android_textAppearance)) {
        val textAppearanceId = it.getResourceId(
            R.styleable.BaselineGridTextView_android_textAppearance,
            android.R.style.TextAppearance)
        context.obtainStyledAttributes(
            textAppearanceId, R.styleable.BaselineGridTextView).use {
          parseTextAttrs(it)
        }
      }

      // then check view attrs
      parseTextAttrs(it)
      maxLinesByHeight = it.getBoolean(R.styleable.BaselineGridTextView_maxLinesByHeight, false)
    }

    FOUR_DIP = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, 4f, resources.displayMetrics)
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
    checkMaxLines(height, MeasureSpec.getMode(heightMeasureSpec))
  }

  private fun parseTextAttrs(a: TypedArray) {
    if (a.hasValue(R.styleable.BaselineGridTextView_lineHeightMultiplierHint)) {
      lineHeightMultiplierHint = a.getFloat(
          R.styleable.BaselineGridTextView_lineHeightMultiplierHint, 1f)
    }
    if (a.hasValue(R.styleable.BaselineGridTextView_lineHeightHint)) {
      lineHeightHint = a.getDimensionPixelSize(
          R.styleable.BaselineGridTextView_lineHeightHint, 0).toFloat()
    }
    if (a.hasValue(R.styleable.BaselineGridTextView_android_fontFamily)) {
      fontResId = a.getResourceId(R.styleable.BaselineGridTextView_android_fontFamily, 0)
    }
  }

  /**
   * Ensures line height is a multiple of 4dp.
   */
  private fun computeLineHeight() {
    val fm = paint.fontMetrics
    val fontHeight = abs(fm.ascent - fm.descent) + fm.leading
    val desiredLineHeight = if (lineHeightHint > 0)
      lineHeightHint
    else
      lineHeightMultiplierHint * fontHeight

    val baselineAlignedLineHeight = (FOUR_DIP * ceil((desiredLineHeight / FOUR_DIP).toDouble()).toFloat() + 0.5f).toInt()
    setLineSpacing(baselineAlignedLineHeight - fontHeight, 1f)
  }

  /**
   * Ensure that the first line of text sits on the 4dp grid.
   */
  private fun ensureBaselineOnGrid(): Int {
    val baseline = baseline.toFloat()
    val gridAlign = baseline % FOUR_DIP
    if (gridAlign != 0f) {
      extraTopPadding = (FOUR_DIP - ceil(gridAlign.toDouble())).toInt()
    }
    return extraTopPadding
  }

  /**
   * Ensure that height is a multiple of 4dp.
   */
  private fun ensureHeightGridAligned(height: Int): Int {
    val gridOverhang = height % FOUR_DIP
    if (gridOverhang != 0f) {
      extraBottomPadding = (FOUR_DIP - ceil(gridOverhang.toDouble())).toInt()
    }
    return extraBottomPadding
  }

  /**
   * When measured with an exact height, text can be vertically clipped mid-line. Prevent
   * this by setting the `maxLines` property based on the available space.
   */
  private fun checkMaxLines(height: Int, heightMode: Int) {
    if (!maxLinesByHeight || heightMode != MeasureSpec.EXACTLY) return

    val textHeight = height - compoundPaddingTop - compoundPaddingBottom
    val completeLines = floor((textHeight / lineHeight).toDouble()).toInt()
    maxLines = completeLines
  }
}
