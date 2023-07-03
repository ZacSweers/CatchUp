// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package dev.zacsweers.catchup.circuit

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.unit.dp
import com.slack.circuit.overlay.Overlay
import com.slack.circuit.overlay.OverlayNavigator
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
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
        confirmValueChange = { newValue ->
          if (hasShown && newValue == SheetValue.Hidden) {
            dismissOnTapOutside
          } else {
            true
          }
        }
      )

    var pendingResult by remember { mutableStateOf<Result?>(null) }
    ModalBottomSheet(
      content = {
        val coroutineScope = rememberCoroutineScope()
        BackHandler(enabled = sheetState.isVisible) {
          coroutineScope
            .launch { sheetState.hide() }
            .invokeOnCompletion {
              if (!sheetState.isVisible) {
                navigator.finish(onDismiss!!.invoke())
              }
            }
        }
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
      shape = RoundedCornerShape(32.dp),
      onDismissRequest = {
        // Only possible if dismissOnTapOutside is false
        check(dismissOnTapOutside)
        navigator.finish(onDismiss!!.invoke())
      },
    )

    LaunchedEffect(model, onDismiss) {
      snapshotFlow { sheetState.currentValue }
        .collect { newValue ->
          if (hasShown && newValue == SheetValue.Hidden) {
            // This is apparently as close as we can get to an "onDismiss" callback, which
            // unfortunately has no animation
            val result = pendingResult ?: onDismiss?.invoke() ?: error("no result!")
            navigator.finish(result)
          }
        }
    }
    LaunchedEffect(model, onDismiss) {
      // TODO why doesn't this ever hit if it's after show()
      hasShown = true
      sheetState.show()
    }
  }
}
