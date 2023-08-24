package dev.zacsweers.catchup.circuit

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.DialogProperties
import com.slack.circuit.overlay.Overlay
import com.slack.circuit.overlay.OverlayNavigator

sealed interface DialogResult {
  data object Confirm : DialogResult

  data object Cancel : DialogResult

  data object Dismiss : DialogResult
}

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
      onDismissRequest = { navigator.finish(DialogResult.Dismiss) },
      icon = icon,
      title = title,
      text = text,
      confirmButton = {
        Button(onClick = { navigator.finish(DialogResult.Confirm) }) { confirmButtonText() }
      },
      dismissButton =
        dismissButtonText?.let { dismissButtonText ->
          {
            Button(
              onClick = {
                navigator.finish(
                  DialogResult.Cancel,
                )
              }
            ) {
              dismissButtonText()
            }
          }
        },
      properties =
        DialogProperties(
          dismissOnClickOutside = dismissOnClickOutside,
          dismissOnBackPress = dismissOnBackPress,
        ),
    )
  }
}
