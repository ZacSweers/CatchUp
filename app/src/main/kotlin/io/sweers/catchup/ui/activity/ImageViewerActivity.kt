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
import androidx.core.view.postDelayed
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.bumptech.glide.Priority.IMMEDIATE
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import io.sweers.catchup.GlideApp
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
import java.security.MessageDigest
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
    setResultAndFinish()
  }

  override fun onNavigateUp(): Boolean {
    setResultAndFinish()
    return true
  }

  private fun loadImage() {
    // Adding a 1px transparent border improves anti-aliasing
    // when the image rotates while being dragged.
    val paddingTransformation = GlidePaddingTransformation(
        paddingPx = 1F,
        paddingColor = Color.TRANSPARENT)

    GlideApp.with(imageView.context)
        .load(url)
        .diskCacheStrategy(DiskCacheStrategy.DATA)
        .transform(paddingTransformation)
        .priority(IMMEDIATE)
        .apply {
          if (id == null) {
            transition(DrawableTransitionOptions.withCrossFade())
          } else {
            dontAnimate()
            listener(object : RequestListener<Drawable> {
              override fun onLoadFailed(
                e: GlideException?,
                model: Any,
                target: Target<Drawable>,
                isFirstResource: Boolean
              ): Boolean {
                startPostponedEnterTransition()
                return false
              }

              override fun onResourceReady(
                resource: Drawable,
                model: Any,
                target: Target<Drawable>,
                dataSource: DataSource,
                isFirstResource: Boolean
              ): Boolean {
                startPostponedEnterTransition()
                return false
              }
            })
          }
        }
        .into(imageView)

    animateDimmingOnEntry()
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
          imageView.postDelayed(flickAnimationDuration) {
            setResultAndFinish()
          }
        } else {
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

  private fun animateDimmingOnEntry() {
    activityBackgroundDrawable = rootLayout.background.mutate()
    rootLayout.background = activityBackgroundDrawable

    ObjectAnimator.ofFloat(1F, 0f).apply {
      duration = 200
      interpolator = FastOutSlowInInterpolator()
      addUpdateListener { animation ->
        updateBackgroundDimmingAlpha(animation.animatedValue as Float)
      }
      start()
    }
  }

  private fun updateBackgroundDimmingAlpha(
    @FloatRange(from = 0.0,
to = 1.0) transparencyFactor: Float
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
private class GlidePaddingTransformation(
  private val paddingPx: Float,
  @ColorInt private val paddingColor: Int
) : BitmapTransformation() {

  override fun transform(
    pool: BitmapPool,
    toTransform: Bitmap,
    outWidth: Int,
    outHeight: Int
  ): Bitmap {
    if (paddingPx == 0F) {
      return toTransform
    }

    val targetWidth = toTransform.width + paddingPx * 2F
    val targetHeight = toTransform.height + paddingPx * 2F

    val bitmapWithPadding = pool.get(targetWidth.toInt(), targetHeight.toInt(),
        Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmapWithPadding)

    val paint = Paint()
    paint.color = paddingColor
    canvas.drawRect(0F, 0F, targetWidth, targetHeight, paint)
    canvas.drawBitmap(toTransform, paddingPx, paddingPx, null)

    return bitmapWithPadding
  }

  override fun updateDiskCacheKey(messageDigest: MessageDigest) {
    messageDigest.update("padding_$paddingPx".toByteArray())
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
