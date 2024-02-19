package catchup.app.service

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dev.zacsweers.catchup.app.scaffold.R

@Composable
fun rememberClickableItemState(
  enabled: Boolean = true,
  contentColor: Color = Color.Unspecified,
): ClickableItemState {
  val colorToUse =
    if (contentColor == Color.Unspecified) {
      contentColorFor(MaterialTheme.colorScheme.surface)
    } else {
      contentColor
    }
  return remember {
    ClickableItemState().apply {
      this.enabled = enabled
      this.contentColor = colorToUse
    }
  }
}

@Stable
class ClickableItemState {
  val interactionSource = MutableInteractionSource()
  var enabled by mutableStateOf(true)
  var contentColor by mutableStateOf(Color.Unspecified)
  var focused by mutableStateOf(false)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ClickableItem(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  onLongClick: (() -> Unit)? = null,
  state: ClickableItemState = rememberClickableItemState(),
  content: @Composable () -> Unit,
) {
  if (state.enabled) {
    val interactionSource = state.interactionSource
    val isPressed by interactionSource.collectIsPressedAsState()
    val targetElevation =
      if (isPressed || state.focused) {
        dimensionResource(R.dimen.touch_raise)
      } else {
        0.dp
      }
    val elevation by animateDpAsState(targetElevation, label = "Animated elevation")
    val haptics = LocalHapticFeedback.current
    Surface(
      modifier =
        modifier
          .combinedClickable(
            interactionSource = interactionSource,
            indication = LocalIndication.current,
            onClick = onClick,
            onLongClick =
              onLongClick?.let { longClickFunction ->
                {
                  haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                  longClickFunction()
                }
              },
          )
          .zIndex(elevation.value),
      shadowElevation = elevation,
      contentColor = state.contentColor,
      content = content,
    )
  } else {
    content()
  }
}
