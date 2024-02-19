package catchup.app.ui.debug

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import catchup.app.data.LumberYard.Entry
import kotlinx.collections.immutable.ImmutableList
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/** A simple list UI for showing debugging logs and sharing them. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LogsList(entries: ImmutableList<Entry>, modifier: Modifier = Modifier, onShare: () -> Unit) {
  LazyColumn(modifier = modifier, contentPadding = PaddingValues(start = 16.dp, end = 16.dp)) {
    stickyHeader(key = "header") {
      Surface {
        Row {
          Text("Logs", fontSize = 24.sp, fontWeight = FontWeight.Bold)
          Spacer(Modifier.weight(1f))
          IconButton(onClick = onShare) {
            Icon(Icons.Filled.Share, contentDescription = "Share logs")
          }
        }
      }
    }
    items(entries.size, key = { it }) { index -> LogEntry(entries[index]) }
  }
}

@Composable
fun LogEntry(entry: Entry, modifier: Modifier = Modifier) {
  Column(modifier = modifier.fillMaxWidth()) {
    Row {
      Text(
        entry.displayLevel,
        modifier = Modifier.width(20.dp).background(backgroundForLevel(entry.level)),
        textAlign = TextAlign.Center,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        fontSize = 10.sp,
      )

      Text(
        entry.tag.orEmpty(),
        modifier = Modifier.weight(1f).padding(PaddingValues(horizontal = 4.dp)),
        maxLines = 1,
        fontSize = 10.sp,
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.typography.labelSmall,
      )
    }

    Text(
      entry.message,
      modifier = Modifier.fillMaxWidth().padding(4.dp),
      style = MaterialTheme.typography.bodySmall,
    )
  }
}

private fun backgroundForLevel(level: Int) =
  when (level) {
    Log.VERBOSE,
    Log.DEBUG -> Color(0xff2196f3)
    Log.INFO -> Color(0xff4caf50)
    Log.WARN -> Color(0xffff9800)
    Log.ERROR,
    Log.ASSERT -> Color(0xfff44336)
    else -> Color(0xff9c27b0)
  }

@Preview
@Composable
private fun LogEntryPreview() {
  LogEntry(
    Entry(
      time = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
      level = Log.DEBUG,
      tag = "CatchUp",
      message = "This is a test message",
    )
  )
}
