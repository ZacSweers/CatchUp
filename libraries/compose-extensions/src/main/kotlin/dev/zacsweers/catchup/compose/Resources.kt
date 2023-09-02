package dev.zacsweers.catchup.compose

import android.content.res.Configuration
import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext

/**
 * A composable function that returns the [Resources]. It will be recomposed when [Configuration]
 * gets updated.
 */
// Copied from Compose, idk why it's internal
@Composable
@ReadOnlyComposable
fun composableResources(): Resources {
  LocalConfiguration.current
  return LocalContext.current.resources
}
