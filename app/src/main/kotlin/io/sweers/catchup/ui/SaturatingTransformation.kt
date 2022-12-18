package io.sweers.catchup.ui

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.graphics.ColorMatrixColorFilter
import android.graphics.drawable.Drawable
import android.view.View
import androidx.core.animation.doOnEnd
import coil.decode.DataSource
import coil.request.ErrorResult
import coil.request.ImageResult
import coil.request.SuccessResult
import coil.transition.Transition
import coil.transition.TransitionTarget
import io.sweers.catchup.base.ui.ImageLoadingColorMatrix
import io.sweers.catchup.util.UiUtil
import kotlin.math.roundToLong

private const val SATURATION_ANIMATION_DURATION = 2000L

/** A [Transition] that saturates and fades in the new drawable on load */
class SaturatingTransformation(
  private val durationMillis: Long = SATURATION_ANIMATION_DURATION,
  private val target: TransitionTarget,
  private val result: ImageResult
) : Transition {
  init {
    require(durationMillis > 0) { "durationMillis must be > 0." }
  }

  override fun transition() {
    // Don't animate if the request was fulfilled by the memory cache.
    if (result is SuccessResult && result.dataSource == DataSource.MEMORY_CACHE) {
      target.onSuccess(result.drawable)
      return
    }

    // Animate the drawable and suspend until the animation is completes.
    when (result) {
      is SuccessResult -> {
        val animator = saturateDrawableAnimator(result.drawable, durationMillis, target.view)
        animator.doOnEnd { animator.cancel() }
        animator.start()

        animator.cancel()
        target.onSuccess(result.drawable)
      }
      is ErrorResult -> target.onError(result.drawable)
    }
  }

  companion object Factory : Transition.Factory {
    override fun create(target: TransitionTarget, result: ImageResult): Transition {
      return SaturatingTransformation(target = target, result = result)
    }
  }
}

private fun saturateDrawableAnimator(
  current: Drawable,
  duration: Long = SATURATION_ANIMATION_DURATION,
  view: View? = null
): Animator {
  current.mutate()
  view?.setHasTransientState(true)

  val cm = ImageLoadingColorMatrix()

  val satAnim = ObjectAnimator.ofFloat(cm, ImageLoadingColorMatrix.PROP_SATURATION, 0f, 1f)
  satAnim.duration = duration
  satAnim.addUpdateListener { current.colorFilter = ColorMatrixColorFilter(cm) }

  val alphaAnim = ObjectAnimator.ofFloat(cm, ImageLoadingColorMatrix.PROP_ALPHA, 0f, 1f)
  alphaAnim.duration = duration / 2

  val darkenAnim = ObjectAnimator.ofFloat(cm, ImageLoadingColorMatrix.PROP_BRIGHTNESS, 0.8f, 1f)
  darkenAnim.duration = (duration * 0.75f).roundToLong()

  return AnimatorSet().apply {
    playTogether(satAnim, alphaAnim, darkenAnim)
    interpolator = UiUtil.fastOutSlowInInterpolator
    doOnEnd {
      current.clearColorFilter()
      view?.setHasTransientState(false)
    }
  }
}
