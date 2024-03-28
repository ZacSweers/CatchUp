package catchup.compose

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

val LocalDynamicTheme = compositionLocalOf { false }

@Suppress("NOTHING_TO_INLINE") // Required in K2
@Composable
inline fun dynamicAwareColor(
  regularColor: @Composable () -> Color,
  dynamicColor: @Composable () -> Color,
  // Trigger recomposition if the context changes
  @Suppress("UNUSED_PARAMETER") context: Context = LocalContext.current,
): Color {
  val isDynamic = LocalDynamicTheme.current
  return if (isDynamic) {
    dynamicColor()
  } else {
    regularColor()
  }
}
