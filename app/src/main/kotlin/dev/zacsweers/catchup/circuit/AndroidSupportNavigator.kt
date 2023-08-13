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

interface AndroidScreen : Screen

@Parcelize
data class IntentScreen(val intent: Intent, val options: Bundle? = null) : AndroidScreen {
  fun launch(activity: Activity) {
    activity.startActivity(intent, options)
  }
}

@Stable
class AndroidSupportNavigator(
  private val delegate: Navigator,
  private val launcher: IntentLauncher,
) : Navigator by delegate {
  override fun goTo(screen: Screen) {
    when (screen) {
      is AndroidScreen -> launcher.launch(screen)
      else -> delegate.goTo(screen)
    }
  }
}

fun interface IntentLauncher {
  fun launch(screen: AndroidScreen)
}

@Composable
fun rememberIntentAwareNavigator(delegate: Navigator, activity: Activity): Navigator {
  val starter =
    remember(activity) {
      IntentLauncher { screen ->
        when (screen) {
          is IntentScreen -> screen.launch(activity)
        }
      }
    }
  return rememberIntentAwareNavigator(delegate, starter)
}

@Composable
fun rememberIntentAwareNavigator(delegate: Navigator, launcher: IntentLauncher): Navigator {
  return remember(delegate) { AndroidSupportNavigator(delegate, launcher) }
}
