package io.sweers.catchup.base.ui

import android.text.format.DateUtils
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.sweers.catchup.util.primaryLocale
import kotlin.time.Duration.Companion.hours
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

// @Composable
// fun TextServiceUi(
//  lazyItems: LazyPagingItems<CatchUpItem>,
//  themeColor: Color,
//  onRefreshChange: (Boolean) -> Unit,
//  eventSink: (ServiceScreen.Event) -> Unit,
// ) {
//  LazyColumn {
//    items(
//      items = lazyItems,
//      key = CatchUpItem::id,
//    ) { item ->
//      ClickableItem(lazyItems, item, eventSink) { TextItem(it, themeColor, eventSink) }
//    }
//    handleLoadStates(lazyItems, themeColor, onRefreshChange)
//  }
// }

@Immutable
data class CatchUpListItem(
  val id: Long,
  val title: String,
  val description: String? = null,
  val score: String? = null,
  val tag: String? = null,
  val author: String? = null,
  val source: String? = null,
  val timestamp: Instant? = null,
  val mark: Mark? = null,
) {
  @Immutable
  data class Mark(val clickable: Boolean, val text: String? = null, val iconPainter: () -> Painter)
}

sealed class ClickEvent(val item: CatchUpListItem) {
  class Item(item: CatchUpListItem) : ClickEvent(item)

  class Mark(item: CatchUpListItem) : ClickEvent(item)
}

@Composable
fun TextItem(
  item: CatchUpListItem,
  themeColor: Color,
  modifier: Modifier = Modifier,
  onClick: (ClickEvent) -> Unit = {}
) {
  Row(
    modifier = modifier.padding(16.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    DetailColumn(item, themeColor)
    item.mark?.let { mark ->
      Column(
        modifier =
          Modifier.padding(start = 16.dp).clickable(
            enabled = mark.clickable,
            interactionSource = remember { MutableInteractionSource() },
            indication = rememberRipple(bounded = false)
          ) {
            onClick(ClickEvent.Mark(item))
          },
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Icon(
          painter = mark.iconPainter(),
          contentDescription = null,
          modifier = Modifier.size(24.dp),
          tint = themeColor
        )
        mark.text?.let { text ->
          Text(text = text, style = MaterialTheme.typography.labelSmall, color = themeColor)
        }
      }
    }
  }
}

@Composable
fun RowScope.DetailColumn(item: CatchUpListItem, themeColor: Color, modifier: Modifier = Modifier) {
  Column(modifier = modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
    // Score, tag, timestamp
    ItemHeader(item, themeColor)
    // Title
    Text(
      text = item.title,
      style = MaterialTheme.typography.titleMedium,
      overflow = TextOverflow.Ellipsis,
    )
    // Description
    item.description?.let {
      Text(
        text = it,
        style = MaterialTheme.typography.bodyMedium,
        overflow = TextOverflow.Ellipsis,
        maxLines = 5,
      )
    }
    // Author, source
    ItemFooter(item)
  }
}

@Composable
private fun ItemHeader(item: CatchUpListItem, themeColor: Color) {
  if (item.score != null || item.tag != null || item.timestamp != null) {
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
      item.score?.let { score ->
        Text(
          text = score,
          fontWeight = FontWeight.Bold,
          style = MaterialTheme.typography.labelSmall,
          color = themeColor
        )
      }
      item.tag?.let { tag ->
        if (item.score != null) {
          Text(
            text = " • ",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelSmall,
            color = themeColor
          )
        }
        val primaryLocale = LocalContext.current.primaryLocale
        Text(
          text =
            tag.replaceFirstChar {
              if (it.isLowerCase()) it.titlecase(primaryLocale) else it.toString()
            },
          fontWeight = FontWeight.Bold,
          style = MaterialTheme.typography.labelSmall,
          color = themeColor
        )
      }
      item.timestamp?.let { timestamp ->
        if (item.score != null || item.tag != null) {
          Text(
            text = " • ",
            style = MaterialTheme.typography.labelSmall,
          )
        }
        val millis = timestamp.toEpochMilliseconds()
        Text(
          text =
            DateUtils.getRelativeTimeSpanString(
                millis,
                System.currentTimeMillis(),
                0L,
                DateUtils.FORMAT_ABBREV_ALL
              )
              .toString(),
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurface
        )
      }
    }
  }
}

@Composable
private fun ItemFooter(item: CatchUpListItem) {
  if (item.author != null || item.source != null) {
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
      // Author
      item.author
        ?.takeUnless { it.isBlank() }
        ?.let { author ->
          Text(
            text = author,
            style = MaterialTheme.typography.labelSmall,
          )
        }
      // Source
      item.source
        ?.takeUnless { it.isBlank() }
        ?.let { source ->
          if (item.author != null) {
            Text(
              text = " — ",
              style = MaterialTheme.typography.labelSmall,
            )
          }
          Text(
            text = source,
            style = MaterialTheme.typography.labelSmall,
          )
        }
    }
  }
}

// fun LazyListScope.handleLoadStates(
//  lazyItems: LazyPagingItems<*>,
//  themeColor: Color,
//  onRefreshChange: (Boolean) -> Unit
// ) {
//  lazyItems.apply {
//    when {
//      loadState.refresh is LoadState.Loading -> {
//        onRefreshChange(true)
//        item { LoadingView(themeColor, Modifier.fillParentMaxSize()) }
//      }
//      loadState.refresh is LoadState.NotLoading -> {
//        onRefreshChange(false)
//      }
//      loadState.append is LoadState.Loading -> {
//        item { LoadingItem() }
//      }
//      loadState.refresh is LoadState.Error -> {
//        val e = loadState.refresh as LoadState.Error
//        item {
//          SideEffect { Timber.e(e) }
//          ErrorItem("Error loading: ${e.error.localizedMessage}", Modifier.fillMaxSize(), ::retry)
//        }
//      }
//      loadState.append is LoadState.Error -> {
//        val e = loadState.append as LoadState.Error
//        item {
//          ErrorItem("Error loading service: ${e.error.localizedMessage}", onRetryClick = ::retry)
//        }
//      }
//    }
//  }
// }

@Preview
@Composable
fun PreviewTextItem() {
  Surface {
    TextItem(
      item =
        CatchUpListItem(
          id = 1L,
          title = "CatchUp: Reborn",
          description =
            "It's been a minute and a lot's changed out there. Time to acquaint CatchUp with 2022.",
          author = "Zac Sweers",
          source = "zacsweers.dev",
          score = "+ 200",
          tag = "News",
          timestamp = Clock.System.now().minus(12.hours),
          mark = null
        ),
      themeColor = Color.Green
    )
  }
}
