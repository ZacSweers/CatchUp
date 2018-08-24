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

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.ViewOutlineProvider
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.res.use
import io.sweers.catchup.R

/**
 * An extension to [ImageView] which has a foreground drawable.
 */
open class ForegroundImageView(context: Context, attrs: AttributeSet)
  : AppCompatImageView(context, attrs) {

  private var foreground: Drawable? = null

  init {
    @Suppress("LeakingThis")
    context.obtainStyledAttributes(attrs, R.styleable.ForegroundView).use {
      setForeground(it.getDrawable(R.styleable.ForegroundView_android_foreground))
    }
    outlineProvider = ViewOutlineProvider.BOUNDS
  }

  override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
    super.onSizeChanged(w, h, oldw, oldh)
    foreground?.run { setBounds(0, 0, w, h) }
  }

  override fun hasOverlappingRendering(): Boolean {
    return false
  }

  override fun verifyDrawable(who: Drawable): Boolean {
    return super.verifyDrawable(who) || who === foreground
  }

  override fun jumpDrawablesToCurrentState() {
    super.jumpDrawablesToCurrentState()
    foreground?.run { jumpToCurrentState() }
  }

  override fun drawableStateChanged() {
    super.drawableStateChanged()
    foreground?.run {
      if (isStateful) {
        state = drawableState
      }
    }
  }

  /**
   * Returns the drawable used as the foreground of this view. The
   * foreground drawable, if non-null, is always drawn on top of the children.
   *
   * @return A Drawable or null if no foreground was set.
   */
  override fun getForeground(): Drawable? {
    return foreground
  }

  /**
   * Supply a Drawable that is to be rendered on top of the contents of this ImageView
   *
   * @param drawable The Drawable to be drawn on top of the ImageView
   */
  @SuppressLint("NewApi")
  override fun setForeground(drawable: Drawable?) {
    if (foreground !== drawable) {
      foreground?.run {
        callback = null
        unscheduleDrawable(this)
      }

      foreground = drawable

      if (foreground != null) {
        foreground!!.setBounds(0, 0, width, height)
        setWillNotDraw(false)
        foreground!!.callback = this
        if (foreground!!.isStateful) {
          foreground!!.state = drawableState
        }
      } else {
        setWillNotDraw(true)
      }
      invalidate()
    }
  }

  override fun draw(canvas: Canvas) {
    super.draw(canvas)
    foreground?.run { draw(canvas) }
  }

  override fun drawableHotspotChanged(x: Float, y: Float) {
    super.drawableHotspotChanged(x, y)
    foreground?.run { setHotspot(x, y) }
  }
}
