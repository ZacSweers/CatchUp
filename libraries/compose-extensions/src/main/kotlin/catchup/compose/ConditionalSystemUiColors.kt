package catchup.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import catchup.base.ui.SystemBarColorController
import catchup.base.ui.rememberSystemBarColorController

class ConditionalSystemUiColors(
  private val systemBarColorController: SystemBarColorController,
  initialStatusBarDarkContent: Boolean,
  initialNavBarDarkContent: Boolean,
) {
  private var storedStatusBarDarkContent by mutableStateOf(initialStatusBarDarkContent)
  private var storedNavBarDarkContent by mutableStateOf(initialNavBarDarkContent)

  fun save() {
    storedStatusBarDarkContent = systemBarColorController.statusBarDarkContentEnabled
    storedNavBarDarkContent = systemBarColorController.navigationBarDarkContentEnabled
  }

  fun restore() {
    systemBarColorController.statusBarDarkContentEnabled = storedStatusBarDarkContent
    systemBarColorController.navigationBarDarkContentEnabled = storedNavBarDarkContent
  }
}

// TODO if dark mode changes during this, it will restore the wrong colors. What do we do?
@Composable
fun rememberConditionalSystemUiColors(
  systemBarColorController: SystemBarColorController = rememberSystemBarColorController()
): ConditionalSystemUiColors {
  return ConditionalSystemUiColors(
    systemBarColorController,
    systemBarColorController.statusBarDarkContentEnabled,
    systemBarColorController.navigationBarDarkContentEnabled,
  )
}
