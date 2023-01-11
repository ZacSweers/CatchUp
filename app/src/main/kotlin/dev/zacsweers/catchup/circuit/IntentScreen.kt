package dev.zacsweers.catchup.circuit

import android.app.Activity
import android.content.Intent
import com.slack.circuit.Navigator
import com.slack.circuit.Screen
import kotlinx.parcelize.Parcelize

@Parcelize data class IntentScreen(val intent: Intent) : Screen

class IntentAwareNavigator(private val activity: Activity, private val delegate: Navigator) :
  Navigator by delegate {
  override fun goTo(screen: Screen) {
    when (screen) {
      is IntentScreen -> {
        activity.startActivity(screen.intent)
      }
      else -> delegate.goTo(screen)
    }
  }
}
