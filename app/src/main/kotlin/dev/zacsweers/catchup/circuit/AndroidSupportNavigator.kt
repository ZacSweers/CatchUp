package dev.zacsweers.catchup.circuit

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.annotation.CheckResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.Screen
import kotlinx.parcelize.Parcelize

/**
 * A [Screen] that refers to a launchable Android component, such as an [Intent].
 *
 * These screens are intercepted by the intercepting [Navigator] returned by
 * [rememberAndroidScreenAwareNavigator] and routed to an [AndroidScreenLauncher] that can launch it
 * natively, such as via [Context.startActivity].
 *
 * This is an interop layer between the [Navigator] API and the Android platform, and not something
 * that should be used to navigate between standard Circuit screens. It should be assumed that
 * calling [Navigator.goTo] with an [AndroidScreen] will result in exiting the current Circuit UI.
 */
interface AndroidScreen : Screen

/**
 * A simple [Intent] [Screen] that wraps an [intent] and optional [options] bundle. These are passed
 * on to [Context.startActivity].
 *
 * For reuse in custom [AndroidScreenLauncher] implementations, you can call [launchWith] on
 * instances of this.
 */
@Parcelize
data class IntentScreen(val intent: Intent, val options: Bundle? = null) : AndroidScreen {
  fun launchWith(context: Context) {
    context.startActivity(intent, options)
  }
}

@Stable
private class AndroidScreenAwareNavigator(
  private val delegate: Navigator,
  private val launcher: AndroidScreenLauncher,
) : Navigator by delegate {
  override fun goTo(screen: Screen) {
    when (screen) {
      is AndroidScreen -> launcher.launch(screen)
      else -> delegate.goTo(screen)
    }
  }
}

/**
 * A custom launcher for [AndroidScreen]s.
 *
 * Implementers should handle launching [AndroidScreen] subtypes, including [IntentScreen] and any
 * user-defined subtypes.
 */
fun interface AndroidScreenLauncher {
  fun launch(screen: AndroidScreen)
}

/**
 * Returns a custom [Navigator] that can navigate to standard Android components like Activities.
 *
 * Note that this overload only handles [IntentScreen] and starting activities. More complex use
 * cases should implement a custom [AndroidScreenLauncher].
 */
@CheckResult
@Composable
fun rememberAndroidScreenAwareNavigator(delegate: Navigator, context: Context): Navigator {
  val starter =
    remember(context) {
      AndroidScreenLauncher { screen ->
        when (screen) {
          is IntentScreen -> screen.launchWith(context)
        }
      }
    }
  return rememberAndroidScreenAwareNavigator(delegate, starter)
}

/**
 * Returns a custom [Navigator] that can navigate to standard Android components like Activities.
 *
 * Note that this overload only handles [IntentScreen] and starting activities. More complex use
 * cases should implement a custom [AndroidScreenLauncher].
 */
@CheckResult
@Composable
fun rememberAndroidScreenAwareNavigator(
  delegate: Navigator,
  launcher: AndroidScreenLauncher
): Navigator = remember(delegate) { AndroidScreenAwareNavigator(delegate, launcher) }
