package dev.zacsweers.catchup.pullrefresh

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.pullRefresh as androidXPullRefresh
import androidx.compose.ui.Modifier

/** @see androidXPullRefresh */
@OptIn(ExperimentalMaterialApi::class)
fun Modifier.pullRefresh(state: PullRefreshState, enabled: Boolean = true) =
  androidXPullRefresh(state.delegate, enabled)

/** @see androidXPullRefresh */
@OptIn(ExperimentalMaterialApi::class)
fun Modifier.pullRefresh(
  onPull: (pullDelta: Float) -> Float,
  onRelease: suspend (flingVelocity: Float) -> Float,
  enabled: Boolean = true
) = androidXPullRefresh(onPull = onPull, onRelease = onRelease, enabled = enabled)
