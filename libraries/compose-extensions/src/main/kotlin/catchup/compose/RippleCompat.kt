@file:Suppress("DEPRECATION_ERROR")

package catchup.compose

import androidx.compose.foundation.Indication
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

@NonRestartableComposable
@Composable
fun rememberRippleCompat(
  bounded: Boolean = true,
  radius: Dp = Dp.Unspecified,
  color: Color = Color.Unspecified,
): Indication = rememberRipple(bounded, radius, color)
