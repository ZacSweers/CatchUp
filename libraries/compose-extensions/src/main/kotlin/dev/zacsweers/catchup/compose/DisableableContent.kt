package dev.zacsweers.catchup.compose

import androidx.compose.material.ContentAlpha
import androidx.compose.material.LocalContentAlpha
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
    LocalContentAlpha provides if (enabled) 1f else ContentAlpha.disabled,
    LocalContentColor provides
      if (enabled) currentContentColor else currentContentColor.copy(alpha = ContentAlpha.disabled),
    LocalEnabled provides enabled,
  ) {
    content()
  }
}
