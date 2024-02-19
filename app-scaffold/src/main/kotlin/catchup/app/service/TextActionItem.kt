package catchup.app.service

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun TextActionItem(
  icon: ImageVector,
  tint: Color,
  contentDescription: String,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  onClick: () -> Unit,
) {
  IconButton(enabled = enabled, modifier = modifier, onClick = onClick) {
    // TODO this always crossfades the initial load for something like bookmarks
    Crossfade(icon, label = "Action item crossfade") {
      Icon(
        imageVector = it,
        contentDescription = contentDescription,
        tint = tint,
        modifier = Modifier.size(24.dp),
      )
    }
  }
}
