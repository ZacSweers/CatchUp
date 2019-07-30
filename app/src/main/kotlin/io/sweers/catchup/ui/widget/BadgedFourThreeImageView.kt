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
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.text.TextPaint
import android.util.AttributeSet
import android.view.Gravity
import androidx.annotation.ColorInt
import androidx.annotation.Px
import androidx.core.content.res.use
import io.sweers.catchup.R
import io.sweers.catchup.util.sdk

/**
 * A view group that draws a badge drawable on top of it's contents.
 */
class BadgedFourThreeImageView(context: Context, attrs: AttributeSet) :
  FourThreeImageView(context, attrs) {

  companion object {
    private const val GIF = "GIF"
    private const val TEXT_SIZE = 12 // sp
    private const val PADDING = 4 // dp
    private const val CORNER_RADIUS = 2 // dp
    private const val BACKGROUND_COLOR = Color.WHITE
    private const val TYPEFACE = "sans-serif-black"
    private const val TYPEFACE_STYLE = Typeface.NORMAL
    private var bitmap: Bitmap? = null
    private var width: Int = 0
    private var height: Int = 0
  }

  var badgeGravity: Int = Gravity.END or Gravity.BOTTOM
  @Px
  var badgePadding: Int = 0

  private val badge: Drawable
  private var drawBadge: Boolean = false
  private var badgeBoundsSet = false

  init {
    badge = GifBadge(context)
    context.obtainStyledAttributes(attrs, R.styleable.BadgedImageView).use {
      badgeGravity = it.getInt(R.styleable.BadgedImageView_catchupBadgeGravity,
          Gravity.END or Gravity.BOTTOM)
      badgePadding = it.getDimensionPixelSize(R.styleable.BadgedImageView_badgePadding, 0)
    }
  }

  fun showBadge(show: Boolean) {
    drawBadge = show
  }

  fun setBadgeColor(@ColorInt color: Int) {
    sdk(29) {
      badge.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
    } ?: run {
      @Suppress("DEPRECATION")
      badge.setColorFilter(color, PorterDuff.Mode.SRC_IN)
    }
  }

  override fun draw(canvas: Canvas) {
    super.draw(canvas)
    if (drawBadge) {
      if (!badgeBoundsSet) {
        layoutBadge()
      }
      badge.draw(canvas)
    }
  }

  override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
    super.onSizeChanged(w, h, oldw, oldh)
    layoutBadge()
  }

  // Here so the IDE stops complaining about not overriding this when using setOnTouchListener >_>
  @Suppress("RedundantOverride")
  override fun performClick(): Boolean {
    return super.performClick()
  }

  private fun layoutBadge() {
    val badgeBounds = badge.bounds
    Gravity.apply(badgeGravity,
        badge.intrinsicWidth,
        badge.intrinsicHeight,
        Rect(0, 0, width, height),
        badgePadding,
        badgePadding,
        badgeBounds)
    badge.bounds = badgeBounds
    badgeBoundsSet = true
  }

  /**
   * A drawable for indicating that an image is animated
   */
  private class GifBadge internal constructor(context: Context) : Drawable() {
    private val paint: Paint

    init {
      if (bitmap == null) {
        val dm = context.resources.displayMetrics
        val density = dm.density
        val scaledDensity = dm.scaledDensity
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG)
        textPaint.typeface = Typeface.create(TYPEFACE, TYPEFACE_STYLE)
        textPaint.textSize = TEXT_SIZE * scaledDensity

        val padding = PADDING * density
        val cornerRadius = CORNER_RADIUS * density
        val textBounds = Rect()
        textPaint.getTextBounds(GIF, 0, GIF.length, textBounds)
        height = (padding + textBounds.height().toFloat() + padding).toInt()
        width = (padding + textBounds.width().toFloat() + padding).toInt()
        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)?.apply {
          setHasAlpha(true)
        }
        val canvas = Canvas(bitmap!!)
        val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        backgroundPaint.color = BACKGROUND_COLOR
        canvas.drawRoundRect(0f, 0f, width.toFloat(), height.toFloat(), cornerRadius, cornerRadius,
            backgroundPaint)
        // punch out the word 'GIF', leaving transparency
        textPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        canvas.drawText(GIF, padding, height - padding, textPaint)
      }
      paint = Paint()
    }

    override fun getIntrinsicWidth(): Int {
      return width
    }

    override fun getIntrinsicHeight(): Int {
      return height
    }

    override fun draw(canvas: Canvas) {
      canvas.drawBitmap(bitmap!!, bounds.left.toFloat(), bounds.top.toFloat(), paint)
    }

    override fun setAlpha(alpha: Int) {
      // ignored
    }

    override fun setColorFilter(cf: ColorFilter?) {
      paint.colorFilter = cf
    }

    override fun getOpacity(): Int {
      return PixelFormat.UNKNOWN
    }
  }
}
