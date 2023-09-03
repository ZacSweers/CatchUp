package catchup.pullrefresh

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshDefaults as AndroidXPullRefreshDefaults
import androidx.compose.material.pullrefresh.PullRefreshState as AndroidXPullRefreshState
import androidx.compose.material.pullrefresh.rememberPullRefreshState as androidXRememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp

/** @see androidXRememberPullRefreshState */
@Composable
@OptIn(ExperimentalMaterialApi::class)
fun rememberPullRefreshState(
  refreshing: Boolean,
  onRefresh: () -> Unit,
  refreshThreshold: Dp = PullRefreshDefaults.RefreshThreshold,
  refreshingOffset: Dp = PullRefreshDefaults.RefreshingOffset,
): PullRefreshState {
  val delegate =
    androidXRememberPullRefreshState(
      refreshing = refreshing,
      onRefresh = onRefresh,
      refreshThreshold = refreshThreshold,
      refreshingOffset = refreshingOffset,
    )
  return PullRefreshState(delegate)
}

/** @see AndroidXPullRefreshState */
@OptIn(ExperimentalMaterialApi::class)
class PullRefreshState internal constructor(internal val delegate: AndroidXPullRefreshState) {
  /** @see AndroidXPullRefreshState.progress */
  val progress
    get() = delegate.progress
}

@OptIn(ExperimentalMaterialApi::class)
object PullRefreshDefaults {
  /** @see AndroidXPullRefreshDefaults.RefreshThreshold */
  val RefreshThreshold = AndroidXPullRefreshDefaults.RefreshThreshold

  /** @see AndroidXPullRefreshDefaults.RefreshingOffset */
  val RefreshingOffset = AndroidXPullRefreshDefaults.RefreshingOffset
}
