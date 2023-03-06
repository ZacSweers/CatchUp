package dev.zacsweers.catchup.pullrefresh

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.pullRefreshIndicatorTransform as androidXPullRefreshIndicatorTransform
import androidx.compose.ui.Modifier

/** @see androidXPullRefreshIndicatorTransform */
@OptIn(ExperimentalMaterialApi::class)
fun Modifier.pullRefreshIndicatorTransform(
  state: PullRefreshState,
  scale: Boolean = false,
) = androidXPullRefreshIndicatorTransform(state.delegate, scale)
