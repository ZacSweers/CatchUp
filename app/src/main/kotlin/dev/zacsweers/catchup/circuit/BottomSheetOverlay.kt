// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package dev.zacsweers.catchup.circuit

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.slack.circuit.overlay.Overlay
import com.slack.circuit.overlay.OverlayNavigator
import dev.zacsweers.catchup.compose.rememberConditionalSystemUiColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
class BottomSheetOverlay<Model : Any, Result : Any>(
  private val model: Model,
  private val dismissOnTapOutside: Boolean = true,
  private val onDismiss: (() -> Result)? = null,
  private val content: @Composable (Model, OverlayNavigator<Result>) -> Unit,
) : Overlay<Result> {
  @Composable
  override fun Content(navigator: OverlayNavigator<Result>) {
    var hasShown by remember { mutableStateOf(false) }
    val sheetState =
      rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        confirmValueChange = { newValue ->
          if (hasShown && newValue == ModalBottomSheetValue.Hidden) {
            dismissOnTapOutside
          } else {
            true
          }
        }
      )

    var pendingResult by remember { mutableStateOf<Result?>(null) }
    ModalBottomSheetLayout(
      modifier = Modifier.fillMaxSize(),
      sheetContent = {
        val coroutineScope = rememberCoroutineScope()
        BackHandler(enabled = sheetState.isVisible) { coroutineScope.launch { sheetState.hide() } }
        // Delay setting the result until we've finished dismissing
        content(model) { result ->
          // This is the OverlayNavigator.finish() callback
          coroutineScope.launch {
            pendingResult = result
            sheetState.hide()
          }
        }
      },
      sheetState = sheetState,
      sheetShape = RoundedCornerShape(32.dp)
    ) {
      // Nothing here, left to the existing content
    }

    val systemUiController = rememberSystemUiController()
    val conditionalSystemUiColors = rememberConditionalSystemUiColors(systemUiController)
    LaunchedEffect(model, onDismiss) {
      snapshotFlow { sheetState.currentValue }
        .collect { newValue ->
          if (hasShown && newValue == ModalBottomSheetValue.Hidden) {
            conditionalSystemUiColors.restore()
            // This is apparently as close as we can get to an "onDismiss" callback, which
            // unfortunately has no animation
            val result = pendingResult ?: onDismiss?.invoke() ?: error("no result!")
            navigator.finish(result)
          } else if (newValue == ModalBottomSheetValue.Expanded) {
            // TODO set status bar colors
            conditionalSystemUiColors.save()
            // TODO don't do this in dark mode?
            systemUiController.statusBarDarkContentEnabled = true
          } else {
            conditionalSystemUiColors.restore()
          }
        }
    }
    LaunchedEffect(model, onDismiss) {
      sheetState.show()
      hasShown = true
    }
  }
}
