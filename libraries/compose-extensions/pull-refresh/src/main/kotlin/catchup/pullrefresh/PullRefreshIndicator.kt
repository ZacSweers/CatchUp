package catchup.pullrefresh

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator as AndroidXPullRefreshIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/** @see AndroidXPullRefreshIndicator */
@Composable
@OptIn(ExperimentalMaterialApi::class)
fun PullRefreshIndicator(
  refreshing: Boolean,
  state: PullRefreshState,
  modifier: Modifier = Modifier,
  backgroundColor: Color = MaterialTheme.colorScheme.surface,
  contentColor: Color = contentColorFor(backgroundColor),
  scale: Boolean = false
) {
  AndroidXPullRefreshIndicator(
    refreshing = refreshing,
    state = state.delegate,
    modifier = modifier,
    backgroundColor = backgroundColor,
    contentColor = contentColor,
    scale = scale,
  )
}
