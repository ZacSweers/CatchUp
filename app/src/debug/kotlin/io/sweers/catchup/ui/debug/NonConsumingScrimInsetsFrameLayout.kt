/*
 * Copyright 2014 Google Inc.
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
package io.sweers.catchup.ui.debug

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.core.view.ViewCompat
import io.sweers.catchup.R

/**
 * A layout that draws something in the insets passed to [.fitSystemWindows], i.e. the
 * area above UI chrome (status and navigation bars, overlay action bars).
 *
 *
 * Unlike the `ScrimInsetsFrameLayout` in the design support library, this variant does not
 * consume the insets.
 */
class NonConsumingScrimInsetsFrameLayout : FrameLayout {
  private var insetForeground: Drawable? = null
  private var insets: Rect? = null
  private val tempRect = Rect()

  constructor(context: Context) : super(context) {
    init(context, null, 0)
  }

  constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
    init(context, attrs, 0)
  }

  constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs,
      defStyle) {
    init(context, attrs, defStyle)
  }

  private fun init(context: Context, attrs: AttributeSet?, defStyle: Int) {
    context.obtainStyledAttributes(attrs, R.styleable.NonConsumingScrimInsetsView, defStyle,
        0)?.run {
      insetForeground = try {
        getDrawable(R.styleable.NonConsumingScrimInsetsView_insetForeground)
      } finally {
        recycle()
      }
    }
    setWillNotDraw(true)
  }

  override fun fitSystemWindows(insets: Rect): Boolean {
    this.insets = Rect(insets)
    setWillNotDraw(insetForeground == null)
    ViewCompat.postInvalidateOnAnimation(this)
    return false // Do not consume insets.
  }

  override fun draw(canvas: Canvas) {
    super.draw(canvas)

    val width = width
    val height = height
    if (insets != null && insetForeground != null) {
      val sc = canvas.save()
      canvas.translate(scrollX.toFloat(), scrollY.toFloat())

      // Top
      tempRect.set(0, 0, width, insets!!.top)
      insetForeground!!.bounds = tempRect
      insetForeground!!.draw(canvas)

      // Bottom
      tempRect.set(0, height - insets!!.bottom, width, height)
      insetForeground!!.bounds = tempRect
      insetForeground!!.draw(canvas)

      // Left
      tempRect.set(0, insets!!.top, insets!!.left, height - insets!!.bottom)
      insetForeground!!.bounds = tempRect
      insetForeground!!.draw(canvas)

      // Right
      tempRect.set(width - insets!!.right, insets!!.top, width, height - insets!!.bottom)
      insetForeground!!.bounds = tempRect
      insetForeground!!.draw(canvas)

      canvas.restoreToCount(sc)
    }
  }

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    insetForeground?.callback = this
  }

  override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()
    insetForeground?.callback = null
  }
}
