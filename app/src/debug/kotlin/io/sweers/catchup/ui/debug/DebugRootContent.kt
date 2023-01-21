package io.sweers.catchup.ui.debug

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.slack.circuit.CircuitContent
import com.squareup.anvil.annotations.ContributesBinding
import dev.zacsweers.catchup.di.AppScope
import dev.zacsweers.catchup.di.SingleIn
import io.sweers.catchup.base.ui.DefaultRootContent
import io.sweers.catchup.base.ui.RootContent
import javax.inject.Inject

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, replaces = [DefaultRootContent::class])
class DebugRootContent @Inject constructor() : RootContent {
  @Composable
  override fun Content(content: @Composable () -> Unit) {
    // TODO show w/ syllabus if not seen before
    val original = LocalLayoutDirection.current
    // This is how to get the drawer on the right and... guh
    val inverted =
      if (original == LayoutDirection.Ltr) {
        LayoutDirection.Rtl
      } else {
        LayoutDirection.Ltr
      }
    CompositionLocalProvider(LocalLayoutDirection provides inverted) {
      val drawerState = rememberDrawerState(DrawerValue.Open)
      // TODO set light icons when drawer is open
      //  val systemUiController = rememberSystemUiController()
      //  systemUiController.systemBarsDarkContentEnabled = drawerState.isOpen
      ModalNavigationDrawer(
        drawerContent = {
          CompositionLocalProvider(LocalLayoutDirection provides original) {
            ModalDrawerSheet(
              windowInsets = WindowInsets(0, 0, 0, 0),
            ) {
              CircuitContent(DebugSettingsScreen)
            }
          }
        },
        drawerState = drawerState
      ) {
        CompositionLocalProvider(LocalLayoutDirection provides original, content = content)
      }
    }
  }
}
