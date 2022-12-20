package dev.zacsweers.catchup.service

import android.text.format.DateUtils
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.items
import dev.zacsweers.catchup.compose.CatchUpTheme
import io.sweers.catchup.R
import io.sweers.catchup.service.api.CatchUpItem
import io.sweers.catchup.service.api.Mark
import io.sweers.catchup.util.kotlin.format
import io.sweers.catchup.util.primaryLocale
import kotlin.time.Duration.Companion.hours
import kotlinx.datetime.Clock

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TextServiceUi(
  lazyItems: LazyPagingItems<CatchUpItem>,
  themeColor: Color,
  onRefreshChange: (Boolean) -> Unit,
  eventSink: (ServiceScreen.Event) -> Unit,
) {
  LazyColumn {
    items(
      items = lazyItems,
      key = CatchUpItem::id,
    ) { item ->
      ClickableItem(
        modifier = Modifier.animateItemPlacement(),
        lazyItems = lazyItems,
        item = item,
        eventSink = eventSink
      ) {
        TextItem(it, themeColor, eventSink)
      }
    }
    handleLoadStates(lazyItems, themeColor, onRefreshChange)
  }
}

@Composable
fun TextItem(item: CatchUpItem, themeColor: Color, eventSink: (ServiceScreen.Event) -> Unit = {}) {
  Row(
    modifier = Modifier.padding(16.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    DetailColumn(item, themeColor)
    item.mark?.let { mark ->
      Column(
        modifier =
          Modifier.padding(start = 16.dp).clickable(
            enabled = item.markClickUrl != null,
            interactionSource = remember { MutableInteractionSource() },
            indication = rememberRipple(bounded = false)
          ) {
            eventSink(ServiceScreen.Event.MarkClicked(item))
          },
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Icon(
          painter = painterResource(mark.icon ?: R.drawable.ic_comment_black_24dp),
          contentDescription = null,
          modifier = Modifier.size(24.dp),
          tint = themeColor
        )
        mark.text?.let { text ->
          val finalText =
            if (mark.formatTextAsCount) {
              text.toLong().format()
            } else text
          Text(
            text = "${mark.textPrefix.orEmpty()}$finalText",
            style = MaterialTheme.typography.labelSmall,
            color = themeColor
          )
        }
      }
    }
  }
}

@Composable
fun RowScope.DetailColumn(item: CatchUpItem, themeColor: Color, modifier: Modifier = Modifier) {
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
private fun ItemHeader(item: CatchUpItem, themeColor: Color) {
  if (item.score != null || item.tag != null || item.timestamp != null) {
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
      item.score?.let { score ->
        Text(
          text = "${score.first} ${score.second.toLong().format()}",
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
private fun ItemFooter(item: CatchUpItem) {
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

fun LazyListScope.handleLoadStates(
  lazyItems: LazyPagingItems<CatchUpItem>,
  themeColor: Color,
  onRefreshChange: (Boolean) -> Unit
) {
  lazyItems.apply {
    when {
      loadState.refresh is LoadState.Loading -> {
        onRefreshChange(true)
        item { LoadingView(themeColor, Modifier.fillParentMaxSize()) }
      }
      loadState.refresh is LoadState.NotLoading -> {
        onRefreshChange(false)
      }
      loadState.append is LoadState.Loading -> {
        item { LoadingItem() }
      }
      loadState.refresh is LoadState.Error -> {
        val e = loadState.refresh as LoadState.Error
        item {
          ErrorItem(
            "Error loading service: ${e.error.localizedMessage}",
            Modifier.fillMaxSize(),
            ::retry
          )
        }
      }
      loadState.append is LoadState.Error -> {
        val e = loadState.append as LoadState.Error
        item {
          ErrorItem("Error loading service: ${e.error.localizedMessage}", onRetryClick = ::retry)
        }
      }
    }
  }
}

@Preview
@Composable
fun PreviewTextItem() {
  CatchUpTheme {
    Surface {
      TextItem(
        item =
          CatchUpItem(
            id = 1L,
            title = "CatchUp: Reborn",
            description =
              "It's been a minute and a lot's changed out there. Time to acquaint CatchUp with 2022.",
            author = "Zac Sweers",
            source = "zacsweers.dev",
            score = "+" to 200,
            tag = "News",
            timestamp = Clock.System.now().minus(12.hours),
            mark = Mark(text = "?")
          ),
        themeColor = colorResource(R.color.colorAccent)
      )
    }
  }
}
