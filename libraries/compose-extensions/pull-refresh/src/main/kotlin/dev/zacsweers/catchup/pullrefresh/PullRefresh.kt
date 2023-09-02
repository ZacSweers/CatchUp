package dev.zacsweers.catchup.pullrefresh

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.pullRefresh as androidXPullRefresh
import androidx.compose.ui.Modifier

/** @see androidXPullRefresh */
@OptIn(ExperimentalMaterialApi::class)
fun Modifier.pullRefresh(state: PullRefreshState, enabled: Boolean = true) =
  androidXPullRefresh(state.delegate, enabled)
