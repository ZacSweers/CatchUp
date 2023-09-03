package catchup.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.accompanist.systemuicontroller.SystemUiController
import com.google.accompanist.systemuicontroller.rememberSystemUiController

class ConditionalSystemUiColors(
  private val systemUiController: SystemUiController,
  initialStatusBarDarkContent: Boolean,
  initialNavBarDarkContent: Boolean,
) {
  private var storedStatusBarDarkContent by mutableStateOf(initialStatusBarDarkContent)
  private var storedNavBarDarkContent by mutableStateOf(initialNavBarDarkContent)

  fun save() {
    storedStatusBarDarkContent = systemUiController.statusBarDarkContentEnabled
    storedNavBarDarkContent = systemUiController.navigationBarDarkContentEnabled
  }

  fun restore() {
    systemUiController.statusBarDarkContentEnabled = storedStatusBarDarkContent
    systemUiController.navigationBarDarkContentEnabled = storedNavBarDarkContent
  }
}

// TODO if dark mode changes during this, it will restore the wrong colors. What do we do?
@Composable
fun rememberConditionalSystemUiColors(
  systemUiController: SystemUiController = rememberSystemUiController()
): ConditionalSystemUiColors {
  return ConditionalSystemUiColors(
    systemUiController,
    systemUiController.statusBarDarkContentEnabled,
    systemUiController.navigationBarDarkContentEnabled
  )
}
