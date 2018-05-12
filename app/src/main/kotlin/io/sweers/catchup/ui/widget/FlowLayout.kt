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

package io.sweers.catchup.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import io.sweers.barber.Barber
import io.sweers.barber.Kind
import io.sweers.barber.StyledAttr
import io.sweers.catchup.R

/**
 * Adapted from a proposal by Chet Haase and Romain Guy in a slide deck somewhere.
 */
class FlowLayout @JvmOverloads constructor(context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0) : ViewGroup(context, attrs, defStyleAttr, defStyleRes) {

  @JvmField
  @StyledAttr(value = R.styleable.FlowLayout_horizontalSpacing, kind = Kind.DIMEN_PIXEL_SIZE)
  var horizontalSpacing: Int = 0

  @JvmField
  @StyledAttr(value = R.styleable.FlowLayout_verticalSpacing, kind = Kind.DIMEN_PIXEL_SIZE)
  var verticalSpacing: Int = 0

  private val paint: Paint

  init {
    Barber.style(this, attrs, R.styleable.FlowLayout, 0, 0)
    paint = Paint()
    paint.isAntiAlias = true
    paint.color = 0xffff0000.toInt()
    paint.strokeWidth = 2.0f
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    val widthSize = View.MeasureSpec.getSize(widthMeasureSpec) - paddingRight
    val widthMode = View.MeasureSpec.getMode(widthMeasureSpec)

    val growHeight = widthMode != View.MeasureSpec.UNSPECIFIED

    var width = 0
    var height = paddingTop

    var currentWidth = paddingLeft
    var currentHeight = 0

    var breakLine = false
    var newLine = false
    var spacing = 0

    val count = childCount
    for (i in 0 until count) {
      val child = getChildAt(i)
      measureChild(child, widthMeasureSpec, heightMeasureSpec)

      val lp = child.layoutParams as LayoutParams
      spacing = horizontalSpacing
      if (lp.horizontalSpacing >= 0) {
        spacing = lp.horizontalSpacing
      }

      if (growHeight && (breakLine || currentWidth + child.measuredWidth > widthSize)) {
        height += currentHeight + verticalSpacing
        currentHeight = 0
        width = Math.max(width, currentWidth - spacing)
        currentWidth = paddingLeft
        newLine = true
      } else {
        newLine = false
      }

      lp.x = currentWidth
      lp.y = height

      currentWidth += child.measuredWidth + spacing
      currentHeight = Math.max(currentHeight, child.measuredHeight)

      breakLine = lp.breakLine
    }

    if (!newLine) {
      height += currentHeight
      width = Math.max(width, currentWidth - spacing)
    }

    width += paddingRight
    height += paddingBottom

    setMeasuredDimension(View.resolveSize(width, widthMeasureSpec),
        View.resolveSize(height, heightMeasureSpec))
  }

  override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    val count = childCount
    for (i in 0 until count) {
      val child = getChildAt(i)
      val lp = child.layoutParams as LayoutParams
      child.layout(lp.x, lp.y, lp.x + child.measuredWidth, lp.y + child.measuredHeight)
    }
  }

  override fun drawChild(canvas: Canvas, child: View, drawingTime: Long): Boolean {
    val more = super.drawChild(canvas, child, drawingTime)
    val lp = child.layoutParams as LayoutParams
    if (lp.horizontalSpacing > 0) {
      val x = child.right.toFloat()
      val y = child.top + child.height / 2.0f
      canvas.drawLine(x, y - 4.0f, x, y + 4.0f, paint)
      canvas.drawLine(x, y, x + lp.horizontalSpacing, y, paint)
      canvas.drawLine(x + lp.horizontalSpacing,
          y - 4.0f,
          x + lp.horizontalSpacing,
          y + 4.0f,
          paint)
    }
    if (lp.breakLine) {
      val x = child.right.toFloat()
      val y = child.top + child.height / 2.0f
      canvas.drawLine(x, y, x, y + 6.0f, paint)
      canvas.drawLine(x, y + 6.0f, x + 6.0f, y + 6.0f, paint)
    }
    return more
  }

  override fun checkLayoutParams(p: ViewGroup.LayoutParams): Boolean {
    return p is LayoutParams
  }

  override fun generateDefaultLayoutParams(): LayoutParams {
    return LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
  }

  override fun generateLayoutParams(attrs: AttributeSet): LayoutParams {
    return LayoutParams(context, attrs)
  }

  override fun generateLayoutParams(p: ViewGroup.LayoutParams): LayoutParams {
    return LayoutParams(p.width, p.height)
  }

  class LayoutParams : ViewGroup.LayoutParams {
    internal var x: Int = 0
    internal var y: Int = 0

    var horizontalSpacing: Int = 0
    var breakLine: Boolean = false

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
      val a = context.obtainStyledAttributes(attrs, R.styleable.FlowLayout_LayoutParams)
      try {
        horizontalSpacing = a.getDimensionPixelSize(
            R.styleable.FlowLayout_LayoutParams_layout_horizontalSpacing,
            -1)
        breakLine = a.getBoolean(R.styleable.FlowLayout_LayoutParams_layout_breakLine, false)
      } finally {
        a.recycle()
      }
    }

    constructor(w: Int, h: Int) : super(w, h)
  }
}
