package catchup.compose

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

@Composable
fun <T> Wigglable(target: T, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
  var triggerWiggle by remember { mutableStateOf(false) }

  LaunchedEffect(target) { triggerWiggle = true }

  // TODO make this better
  val angle by
    animateFloatAsState(
      targetValue = if (triggerWiggle) 10f else 0f,
      animationSpec =
        tween(
          durationMillis = 200,
        ),
      label = "Wiggle"
    )

  LaunchedEffect(angle) {
    if (angle == 10f) {
      triggerWiggle = false
    }
  }

  Box(modifier = modifier.graphicsLayer(rotationZ = angle)) { content() }
}
