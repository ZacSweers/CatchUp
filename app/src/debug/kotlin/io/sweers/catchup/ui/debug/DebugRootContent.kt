package io.sweers.catchup.ui.debug

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.slack.circuit.foundation.CircuitContent
import com.slack.circuit.foundation.onNavEvent
import com.slack.circuit.runtime.Navigator
import com.squareup.anvil.annotations.ContributesBinding
import catchup.compose.rememberConditionalSystemUiColors
import catchup.di.AppScope
import catchup.di.SingleIn
import catchup.base.ui.DefaultRootContent
import catchup.base.ui.RootContent
import io.sweers.catchup.data.DebugPreferences
import javax.inject.Inject

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, replaces = [DefaultRootContent::class])
class DebugRootContent
@Inject
constructor(
  private val debugPreferences: DebugPreferences,
) : RootContent {
  @Composable
  override fun Content(navigator: Navigator, content: @Composable () -> Unit) {
    val original = LocalLayoutDirection.current
    // This is how to get the drawer on the right and... guh
    val inverted =
      if (original == LayoutDirection.Ltr) {
        LayoutDirection.Rtl
      } else {
        LayoutDirection.Ltr
      }
    CompositionLocalProvider(LocalLayoutDirection provides inverted) {
      val drawerState = rememberDrawerState(DrawerValue.Closed)
      // TODO show w/ syllabus if not seen before
      LaunchedEffect(drawerState) {
        // Wait until fully drawn
        withFrameNanos {}
        debugPreferences.seenDebugDrawer.collect { hasSeen ->
          if (!hasSeen) {
            debugPreferences.edit { it[DebugPreferences.Keys.seenDebugDrawer] = true }
            drawerState.open()
          }
        }
      }

      // Update system bar icon colors when drawer is open/closed
      val systemUiController = rememberSystemUiController()
      val conditionalSystemUiColors = rememberConditionalSystemUiColors(systemUiController)
      LaunchedEffect(systemUiController) {
        snapshotFlow { drawerState.currentValue }
          .collect { value ->
            when (value) {
              DrawerValue.Closed -> {
                conditionalSystemUiColors.restore()
              }
              DrawerValue.Open -> {
                conditionalSystemUiColors.save()
                systemUiController.systemBarsDarkContentEnabled = false
              }
            }
          }
      }
      ModalNavigationDrawer(
        drawerContent = {
          CompositionLocalProvider(LocalLayoutDirection provides original) {
            ModalDrawerSheet(
              windowInsets = WindowInsets(0, 0, 0, 0),
            ) {
              CircuitContent(screen = DebugSettingsScreen, onNavEvent = navigator::onNavEvent)
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
