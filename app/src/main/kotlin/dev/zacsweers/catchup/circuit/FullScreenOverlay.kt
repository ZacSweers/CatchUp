package dev.zacsweers.catchup.circuit

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.slack.circuit.CircuitContent
import com.slack.circuit.Navigator
import com.slack.circuit.Screen
import com.slack.circuit.onNavEvent
import com.slack.circuit.overlay.Overlay
import com.slack.circuit.overlay.OverlayNavigator
import dev.zacsweers.catchup.compose.ConditionalSystemUiColors
import dev.zacsweers.catchup.compose.rememberConditionalSystemUiColors

class FullScreenOverlay<S : Screen>(private val screen: S) : Overlay<Unit> {
  @Composable
  override fun Content(navigator: OverlayNavigator<Unit>) {
    val systemUiController = rememberSystemUiController()
    val conditionalSystemUiColors = rememberConditionalSystemUiColors(systemUiController)
    val dispatchingNavigator = remember {
      DispatchingOverlayNavigator(navigator, conditionalSystemUiColors)
    }
    // TODO why doesn't this work?? Back still quits the activity
    BackHandler(enabled = true, onBack = dispatchingNavigator::pop)
    CircuitContent(screen = screen, onNavEvent = dispatchingNavigator::onNavEvent)
  }
}

private class DispatchingOverlayNavigator(
  private val navigator: OverlayNavigator<Unit>,
  private val conditionalSystemUiColors: ConditionalSystemUiColors,
) : Navigator {
  override fun goTo(screen: Screen) {
    error("goTo() is not supported in full screen overlays!")
  }

  override fun pop(): Screen? {
    navigator.finish(Unit)
    conditionalSystemUiColors.restore()
    return null
  }

  override fun resetRoot(newRoot: Screen): List<Screen> {
    error("resetRoot() is not supported in full screen overlays!")
  }
}
