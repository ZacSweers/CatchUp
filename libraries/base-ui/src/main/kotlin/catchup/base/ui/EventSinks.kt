package catchup.base.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisallowComposableCalls
import androidx.compose.runtime.remember
import com.slack.circuit.runtime.CircuitUiEvent

@Suppress("NOTHING_TO_INLINE")
@Composable
inline fun <UiEvent : CircuitUiEvent> rememberEventSink(
  noinline body: @DisallowComposableCalls (UiEvent) -> Unit
): (UiEvent) -> Unit {
  return remember(calculation = { body })
}
