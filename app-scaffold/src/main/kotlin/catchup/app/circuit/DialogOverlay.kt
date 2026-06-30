/*
 * Copyright (C) 2026. Zac Sweers
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package catchup.app.circuit

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.window.DialogProperties
import catchup.app.circuit.DialogResult.Cancel
import catchup.app.circuit.DialogResult.Confirm
import catchup.app.circuit.DialogResult.Dismiss
import com.slack.circuit.overlay.Overlay
import com.slack.circuit.overlay.OverlayNavigator

sealed interface DialogResult {
  data object Confirm : DialogResult

  data object Cancel : DialogResult

  data object Dismiss : DialogResult
}

@Stable // TODO remove in next circuit release
class DialogOverlay(
  private val confirmButtonText: @Composable () -> Unit,
  private val icon: @Composable (() -> Unit)? = null,
  private val title: @Composable (() -> Unit)? = null,
  private val text: @Composable (() -> Unit)? = null,
  private val dismissButtonText: (@Composable () -> Unit)?,
  private val dismissOnBackPress: Boolean = true,
  private val dismissOnClickOutside: Boolean = true,
) : Overlay<DialogResult> {
  @Composable
  override fun Content(navigator: OverlayNavigator<DialogResult>) {
    AlertDialog(
      onDismissRequest = { navigator.finish(Dismiss) },
      icon = icon,
      title = title,
      text = text,
      confirmButton = { Button(onClick = { navigator.finish(Confirm) }) { confirmButtonText() } },
      dismissButton =
        dismissButtonText?.let { dismissButtonText ->
          { Button(onClick = { navigator.finish(Cancel) }) { dismissButtonText() } }
        },
      properties =
        DialogProperties(
          dismissOnClickOutside = dismissOnClickOutside,
          dismissOnBackPress = dismissOnBackPress,
        ),
    )
  }
}
