package dev.zacsweers.catchup.circuit

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import com.slack.circuit.backstack.NavDecoration

object DefaultCircuitNavDecoration : NavDecoration {
  private const val FIVE_PERCENT = 0.05f
  private val SlightlyRight = { width: Int -> (width * FIVE_PERCENT).toInt() }
  private val SlightlyLeft = { width: Int -> 0 - (width * FIVE_PERCENT).toInt() }

  @Composable
  override fun <T> DecoratedContent(
    arg: T,
    backStackDepth: Int,
    modifier: Modifier,
    content: @Composable (T) -> Unit
  ) {
    // Remember the previous stack depth so we know if the navigation is going "back".
    val prevStackDepth = rememberSaveable { mutableStateOf(backStackDepth) }
    val diff = backStackDepth - prevStackDepth.value
    prevStackDepth.value = backStackDepth
    AnimatedContent(
      targetState = arg,
      modifier = modifier,
      transitionSpec = {
        // Mirror the forward and backward transitions of activities in Android 33
        if (diff > 0) {
          slideInHorizontally(tween(), SlightlyRight) + fadeIn() togetherWith
            slideOutHorizontally(tween(), SlightlyLeft) + fadeOut()
        } else
          if (diff < 0) {
              slideInHorizontally(tween(), SlightlyLeft) + fadeIn() togetherWith
                slideOutHorizontally(tween(), SlightlyRight) + fadeOut()
            } else {
              // Crossfade if there was no diff
              fadeIn() togetherWith fadeOut()
            }
            .using(
              // Disable clipping since the faded slide-in/out should
              // be displayed out of bounds.
              SizeTransform(clip = false)
            )
      },
      label = "Nav transition",
    ) {
      content(it)
    }
  }
}
