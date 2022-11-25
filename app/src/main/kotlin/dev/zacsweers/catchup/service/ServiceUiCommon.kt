package dev.zacsweers.catchup.service

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import io.sweers.catchup.R
import io.sweers.catchup.service.api.CatchUpItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectableItem(
  lazyItems: LazyPagingItems<CatchUpItem>,
  item: CatchUpItem?,
  eventSink: (ServiceScreen.Event) -> Unit,
  content: @Composable (CatchUpItem) -> Unit
) {
  if (item == null) {
    ErrorItem("Item was null!") { lazyItems.retry() }
  } else {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val targetElevation =
      if (isPressed) {
        dimensionResource(R.dimen.touch_raise)
      } else {
        0.dp
      }
    val elevation by animateDpAsState(targetElevation)
    Surface(
      tonalElevation = elevation,
      shadowElevation = elevation,
      interactionSource = interactionSource,
      onClick = { eventSink(ServiceScreen.Event.ItemClicked(item)) }
    ) {
      content(item)
    }
  }
}
