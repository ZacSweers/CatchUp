package dev.zacsweers.catchup.circuit

import android.app.Activity
import android.content.Intent
import androidx.compose.runtime.Stable
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.Screen
import io.sweers.catchup.util.maybeStartChooser
import kotlinx.parcelize.Parcelize

@Parcelize data class IntentScreen(val intent: Intent, val isChooser: Boolean = false) : Screen

// TODO this leaks the activity after destroy somehow
@Stable
class IntentAwareNavigator(private val activity: Activity, private val delegate: Navigator) :
  Navigator by delegate {
  override fun goTo(screen: Screen) {
    when (screen) {
      is IntentScreen -> {
        val intent = screen.intent
        if (screen.isChooser) {
          activity.maybeStartChooser(intent)
        } else {
          activity.startActivity(intent)
        }
      }
      else -> delegate.goTo(screen)
    }
  }
}
