package dev.zacsweers.catchup.compose

import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf

val LocalEnabled = compositionLocalOf { true }

// TODO this is super hacky but compose's built-in support is patchy at best
@Composable
fun DisableableContent(
  enabled: Boolean,
  content: @Composable () -> Unit,
) {
  val currentContentColor = LocalContentColor.current
  CompositionLocalProvider(
    // Magic value lifted from
    // https://developer.android.com/jetpack/compose/designsystems/material2-material3#emphasis-and
    LocalContentColor provides
      if (enabled) currentContentColor else currentContentColor.copy(alpha = 0.38f),
    LocalEnabled provides enabled,
  ) {
    content()
  }
}
