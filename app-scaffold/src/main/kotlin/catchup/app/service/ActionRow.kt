package catchup.app.service

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import catchup.app.service.bookmarks.BookmarkIconScreen
import com.slack.circuit.foundation.CircuitContent

@Composable
fun ActionRow(
  itemId: Long,
  themeColor: Color,
  modifier: Modifier = Modifier,
  onShareClick: () -> Unit = {},
  canBeSummarized: Boolean = false,
  onSummarizeClick: () -> Unit = {},
) {
  Row(
    horizontalArrangement = Arrangement.SpaceEvenly,
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier.fillMaxWidth(),
  ) {
    val bookmarkIconScreen = remember(itemId) { BookmarkIconScreen(itemId, themeColor.toArgb()) }
    CircuitContent(bookmarkIconScreen)
    TextActionItem(Icons.Filled.Share, themeColor, "Share") { onShareClick() }
    TextActionItem(Icons.Filled.Info, themeColor, "Summarize", enabled = canBeSummarized) {
      onSummarizeClick()
    }
  }
}
