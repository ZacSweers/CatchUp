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
package io.sweers.catchup.ui.activity

import android.animation.Animator
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.transition.Transition
import android.transition.TransitionValues
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.annotation.ColorInt
import androidx.annotation.FloatRange
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.animation.addListener
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import coil.api.load
import coil.transform.Transformation
import io.sweers.catchup.R
import io.sweers.catchup.ui.immersive.SystemUiHelper
import io.sweers.catchup.ui.widget.ZoomableGestureImageView
import io.sweers.catchup.util.toggleVisibility
import kotterknife.bindView
import me.saket.flick.ContentSizeProvider
import me.saket.flick.FlickCallbacks
import me.saket.flick.FlickDismissLayout
import me.saket.flick.FlickGestureListener
import me.saket.flick.InterceptResult
import kotlin.math.abs
import kotlin.math.min

class ImageViewerActivity : AppCompatActivity() {

  companion object {
    const val INTENT_ID = "imageviewer.id"
    const val INTENT_URL = "imageviewer.url"
    const val INTENT_SOURCE_URL = "imageviewer.sourceUrl"
    const val RETURN_IMAGE_ID = "imageviewer.returnimageid"

    fun intent(context: Context, url: String): Intent {
      return Intent(context, ImageViewerActivity::class.java).putExtra("url", url)
    }

    fun targetImageUrl(intent: Intent): String? {
      return intent.getStringExtra(INTENT_URL)
    }

    fun targetImageId(intent: Intent): String? {
      return intent.getStringExtra(INTENT_ID)
    }

    fun sourceUrl(intent: Intent): String? {
      return intent.getStringExtra(INTENT_SOURCE_URL)
    }
  }

  private val rootLayout by bindView<ViewGroup>(R.id.imageviewer_root)
  private val imageView by bindView<ZoomableGestureImageView>(R.id.image)
  private val sourceButton by bindView<AppCompatImageButton>(R.id.image_source)
  private val flickDismissLayout by bindView<FlickDismissLayout>(R.id.imageviewer_image_container)

  private lateinit var systemUiHelper: SystemUiHelper
  private lateinit var activityBackgroundDrawable: Drawable
  private lateinit var url: String
  private var id: String? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
    super.onCreate(savedInstanceState)

    url = targetImageUrl(intent) ?: run {
      finishAfterTransition()
      return
    }

    id = targetImageId(intent)

    setContentView(R.layout.activity_image_viewer)

    sourceUrl(intent)?.let { source ->
      sourceButton.setOnClickListener {
        // TODO bring in LinkManager here
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(source)))
      }
    }

    loadImage()

    flickDismissLayout.gestureListener = flickGestureListener()

    systemUiHelper = SystemUiHelper(this, SystemUiHelper.LEVEL_IMMERSIVE, 0, null)
    imageView.setOnClickListener {
      systemUiHelper.toggle()
      sourceButton.toggleVisibility(animate = true)
    }
  }

  override fun onBackPressed() {
    animateDimmingEnterExit(activityBackgroundDrawable.alpha, 0, 300)
    setResultAndFinish()
  }

  override fun onNavigateUp(): Boolean {
    onBackPressed()
    return true
  }

  private fun loadImage() {
    imageView.load(url) {
      // Adding a 1px transparent border improves anti-aliasing
      // when the image rotates while being dragged.
      transformations(CoilPaddingTransformation(
          paddingPx = 1F,
          paddingColor = Color.TRANSPARENT
      ))
      if (id == null) {
        crossfade(true)
      } else {
        crossfade(false)
        listener(
            onError = { _, _ ->
              startPostponedEnterTransition()
            },
            onSuccess = { _, _ ->
              startPostponedEnterTransition()
            }
        )
      }
    }

    activityBackgroundDrawable = rootLayout.background
    rootLayout.background = activityBackgroundDrawable
    animateDimmingEnterExit(0, 255, 300)
    if (id != null) {
      postponeEnterTransition()
    }
  }

  private fun setResultAndFinish() {
    val resultData = Intent().apply {
      putExtra(RETURN_IMAGE_ID, id)
    }
    setResult(Activity.RESULT_OK, resultData)
    finishAfterTransition()
  }

  private fun flickGestureListener(): FlickGestureListener {
    val contentHeightProvider = object : ContentSizeProvider {
      override fun heightForDismissAnimation(): Int {
        return imageView.zoomedImageHeight.toInt()
      }

      // A positive height value is important so that the user
      // can dismiss even while the progress indicator is visible.
      override fun heightForCalculatingDismissThreshold(): Int {
        return when {
          imageView.drawable == null -> resources.getDimensionPixelSize(
              R.dimen.media_image_height_when_empty)
          else -> imageView.visibleZoomedImageHeight.toInt()
        }
      }
    }

    val callbacks = object : FlickCallbacks {
      @SuppressLint("NewApi")
      override fun onFlickDismiss(flickAnimationDuration: Long) {
//        val rotation = flickDismissLayout.rotation
//        flickDismissLayout.rotation = 0f
//        imageView.rotation = rotation
//        window.sharedElementReturnTransition.let { originalTransition ->
//          window.sharedElementReturnTransition = TransitionSet().addTransition(originalTransition)
//              .addTransition(Rotate().apply {
//                captureStartValues(TransitionValues().apply {
//                  values["catchup:imagereturn:rotation"] = flickDismissLayout.rotation
//                  view = imageView
//                })
//                captureEndValues(TransitionValues().apply {
//                  values["catchup:imagereturn:rotation"] = 0
//                  view = imageView
//                })
//              })
// //              .addTarget(originalTransition.targetIds.first())
//              .setDuration(originalTransition.duration)
//              .setInterpolator(originalTransition.interpolator)
//        }
        if (id == null) {
          animateDimmingEnterExit(activityBackgroundDrawable.alpha, 0, flickAnimationDuration) {
            finish()
          }
        } else {
          animateDimmingEnterExit(activityBackgroundDrawable.alpha, 0, flickAnimationDuration)
          setResultAndFinish()
        }
      }

      override fun onMove(@FloatRange(from = -1.0, to = 1.0) moveRatio: Float) {
        updateBackgroundDimmingAlpha(abs(moveRatio))
      }
    }

    val gestureListener = FlickGestureListener(this, contentHeightProvider, callbacks)

    // Block flick gestures if the image can pan further.
    gestureListener.gestureInterceptor = { scrollY ->
      val isScrollingUpwards = scrollY < 0
      val directionInt = if (isScrollingUpwards) -1 else +1
      val canPanFurther = imageView.canScrollVertically(directionInt)

      when {
        canPanFurther -> InterceptResult.INTERCEPTED
        else -> InterceptResult.IGNORED
      }
    }

    return gestureListener
  }

  private fun animateDimmingEnterExit(
      start: Int,
      end: Int,
      duration: Long,
      onEnd: ((animator: Animator) -> Unit)? = null) {
    ObjectAnimator.ofInt(start, end).apply {
      setDuration(duration)
      interpolator = FastOutSlowInInterpolator()
      addUpdateListener { animation ->
        activityBackgroundDrawable.alpha = animation.animatedValue as Int
        sourceButton.imageAlpha = animation.animatedValue as Int
      }
      onEnd?.let {
        addListener(onEnd = it)
      }
      start()
    }
  }

  private fun updateBackgroundDimmingAlpha(
    @FloatRange(from = 0.0, to = 1.0) transparencyFactor: Float
  ) {
    // Increase dimming exponentially so that the background is
    // fully transparent while the image has been moved by half.
    val dimming = 1f - min(1f, transparencyFactor * 2)
    val finalAlpha = (dimming * 255).toInt()
    activityBackgroundDrawable.alpha = finalAlpha
    sourceButton.imageAlpha = finalAlpha
  }
}

/** Adds a solid padding around an image. */
private class CoilPaddingTransformation(
    private val paddingPx: Float,
    @ColorInt private val paddingColor: Int
) : Transformation {
  override fun key(): String = "padding_$paddingPx"

  override suspend fun transform(pool: coil.bitmappool.BitmapPool, input: Bitmap): Bitmap {
    if (paddingPx == 0F) {
      return input
    }

    val targetWidth = input.width + paddingPx * 2F
    val targetHeight = input.height + paddingPx * 2F

    val bitmapWithPadding = pool.get(targetWidth.toInt(), targetHeight.toInt(),
        Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmapWithPadding)

    val paint = Paint()
    paint.color = paddingColor
    canvas.drawRect(0F, 0F, targetWidth, targetHeight, paint)
    canvas.drawBitmap(input, paddingPx, paddingPx, null)

    return bitmapWithPadding
  }
}

/**
 * This transition captures the rotation property of targets before and after
 * the scene change and animates any changes.
 *
 * Copied from AOSP because it's private there for some reason
 */
class Rotate : Transition() {

  override fun captureStartValues(transitionValues: TransitionValues) {
    transitionValues.values[PROPNAME_ROTATION] = transitionValues.view.rotation
  }

  override fun captureEndValues(transitionValues: TransitionValues) {
    transitionValues.values[PROPNAME_ROTATION] = transitionValues.view.rotation
  }

  override fun createAnimator(
    sceneRoot: ViewGroup,
    startValues: TransitionValues?,
    endValues: TransitionValues?
  ): Animator? {
    if (startValues == null || endValues == null) {
      return null
    }
    val view = endValues.view
    val startRotation = startValues.values[PROPNAME_ROTATION] as Float
    val endRotation = endValues.values[PROPNAME_ROTATION] as Float
    if (startRotation != endRotation) {
      view.rotation = startRotation
      return ObjectAnimator.ofFloat(view, View.ROTATION,
          startRotation, endRotation)
    }
    return null
  }

  companion object {

    private const val PROPNAME_ROTATION = "android:rotate:rotation"
  }
}
