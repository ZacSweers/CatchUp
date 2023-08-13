package dev.zacsweers.catchup.circuit

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.Screen
import kotlinx.parcelize.Parcelize

@Parcelize data class IntentScreen(val intent: Intent, val options: Bundle? = null) : Screen

@Stable
class AndroidSupportNavigator(
  private val delegate: Navigator,
  private val handler: IntentHandler,
) : Navigator by delegate {
  override fun goTo(screen: Screen) {
    when (screen) {
      is IntentScreen -> handler.handle(screen.intent, screen.options)
      else -> delegate.goTo(screen)
    }
  }
}

fun interface IntentHandler {
  fun handle(intent: Intent, options: Bundle?)
}

@Composable
fun rememberIntentAwareNavigator(delegate: Navigator, activity: Activity): Navigator {
  val starter = remember(activity) { IntentHandler(activity::startActivity) }
  return rememberIntentAwareNavigator(delegate, starter)
}

@Composable
fun rememberIntentAwareNavigator(delegate: Navigator, starter: IntentHandler): Navigator {
  return remember(delegate) { AndroidSupportNavigator(delegate, starter) }
}
