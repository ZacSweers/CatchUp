package catchup.app.service

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.text.format.DateUtils
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import catchup.app.service.ServiceScreen.Event
import catchup.app.service.ServiceScreen.Event.ItemActionClicked
import catchup.app.service.ServiceScreen.Event.ItemActionClicked.Action.SHARE
import catchup.app.service.ServiceScreen.Event.ItemActionClicked.Action.SUMMARIZE
import catchup.app.service.ServiceScreen.Event.ItemClicked
import catchup.app.service.ServiceScreen.Event.MarkClicked
import catchup.compose.CatchUpTheme
import catchup.compose.ContentAlphas
import catchup.compose.ScrollToTopHandler
import catchup.service.api.CatchUpItem
import catchup.service.api.Mark
import catchup.service.api.canBeSummarized
import catchup.util.kotlin.format
import catchup.util.primaryLocale
import com.slack.circuit.retained.rememberRetained
import dev.zacsweers.catchup.app.scaffold.R
import kotlin.time.Duration.Companion.hours
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

@Composable
fun TextServiceUi(
  lazyItems: LazyPagingItems<CatchUpItem>,
  themeColor: Color,
  eventSink: (Event) -> Unit,
  modifier: Modifier = Modifier,
) {
  val state = rememberLazyListState()
  ScrollToTopHandler(state)

  // Only animate items in on first load
  var animatePlacement by rememberRetained { mutableStateOf(true) }
  var expandedItemIndex by rememberRetained { mutableIntStateOf(-1) }
  LazyColumn(modifier = modifier, state = state) {
    items(
      count = lazyItems.itemCount,
      // Here we use the new itemKey extension on LazyPagingItems to
      // handle placeholders automatically, ensuring you only need to provide
      // keys for real items
      key = lazyItems.itemKey { it.id },
    ) { index ->
      val item = lazyItems[index]
      if (item == null) {
        PlaceholderItem(themeColor)
      } else {
        val itemModifier =
          if (animatePlacement) {
            LaunchedEffect(Unit) { animatePlacement = false }
            Modifier.animateItem()
          } else {
            Modifier
          }
        val clickableItemState = rememberClickableItemState()
        clickableItemState.focused = expandedItemIndex == index
        val haptic = LocalHapticFeedback.current
        ClickableItem(
          modifier = itemModifier,
          state = clickableItemState,
          onClick = { eventSink(ItemClicked(item)) },
          onLongClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            expandedItemIndex = if (expandedItemIndex == index) -1 else index
          },
        ) {
          Column(Modifier.animateContentSize()) {
            TextItem(item, themeColor) { eventSink(MarkClicked(item)) }
            if (index == expandedItemIndex) {
              ActionRow(
                modifier = Modifier.padding(bottom = 8.dp),
                itemId = item.id,
                themeColor = themeColor,
                onShareClick = { eventSink(ItemActionClicked(item, SHARE)) },
                canBeSummarized = item.canBeSummarized,
                onSummarizeClick = { eventSink(ItemActionClicked(item, SUMMARIZE)) },
              )
            }
          }
        }
      }
    }
    handleLoadStates(lazyItems, themeColor)
  }
}

@Composable
fun PlaceholderItem(themeColor: Color) {
  return TextItem(
    CatchUpItem(
      id = -1L,
      title = "Placeholder with some text",
      description = "Placeholder with some longer text to fill the lines up",
      author = "Placeholder",
      tag = "Placeholder",
    ),
    themeColor,
  )
}

@Composable
fun TextItem(
  item: CatchUpItem,
  themeColor: Color,
  modifier: Modifier = Modifier,
  showDescription: Boolean = true,
  onMarkClick: () -> Unit = {},
) {
  Row(modifier = modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
    DetailColumn(item, themeColor, showDescription = showDescription)
    item.mark?.let { mark ->
      Column(
        modifier =
          Modifier.padding(start = 16.dp)
            .clickable(
              enabled = item.markClickUrl != null,
              interactionSource = null,
              indication = ripple(bounded = false),
              onClick = onMarkClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        val icon =
          when (mark.markType) {
            Mark.MarkType.COMMENT -> ImageVector.vectorResource(R.drawable.ic_comment_black_24dp)
            Mark.MarkType.STAR -> Icons.Filled.Star
          }
        Icon(
          painter = rememberVectorPainter(icon),
          contentDescription = null,
          modifier = Modifier.size(24.dp),
          tint = mark.iconTintColor?.let(::Color) ?: themeColor,
        )
        mark.text?.let { text ->
          val finalText =
            if (mark.formatTextAsCount) {
              text.toLong().format()
            } else text
          Text(
            text = "${mark.textPrefix.orEmpty()}$finalText",
            style = MaterialTheme.typography.labelSmall,
            color = themeColor,
          )
        }
      }
    }
  }
}

@Composable
fun RowScope.DetailColumn(
  item: CatchUpItem,
  themeColor: Color,
  modifier: Modifier = Modifier,
  showDescription: Boolean = true,
) {
  Column(modifier = modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
    // Score, tag, timestamp
    ItemHeader(item, themeColor)
    // Title
    Text(
      text = item.title,
      style = MaterialTheme.typography.titleMedium,
      overflow = TextOverflow.Ellipsis,
      color = MaterialTheme.colorScheme.onSurface,
    )
    // Description
    item.description
      ?.takeIf { showDescription }
      ?.let {
        Text(
          text = it,
          style = MaterialTheme.typography.bodyMedium,
          overflow = TextOverflow.Ellipsis,
          maxLines = 5,
          color = MaterialTheme.colorScheme.onSurface.copy(alpha = ContentAlphas.Medium),
        )
      }
    // Author, source
    ItemFooter(item)
  }
}

@Composable
private fun ItemHeader(item: CatchUpItem, themeColor: Color, modifier: Modifier = Modifier) {
  TextItemHeader(
    score = item.score?.second?.toLong(),
    scorePrefix = item.score?.first?.let { "$it " },
    tag = item.tag,
    tagHintColor = item.tagHintColor?.let(::Color) ?: themeColor,
    timestamp = item.timestamp,
    themeColor = themeColor,
    modifier = modifier,
  )
}

@Composable
fun TextItemHeader(
  score: Long?,
  tag: String?,
  timestamp: Instant?,
  modifier: Modifier = Modifier,
  scorePrefix: String? = null,
  themeColor: Color = LocalContentColor.current,
  tagHintColor: Color = themeColor,
) {
  if (score != null || tag != null || timestamp != null) {
    Row(
      modifier = modifier,
      verticalAlignment = Alignment.Bottom,
      horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      score?.let { score ->
        Text(
          text = "${scorePrefix.orEmpty()}${score.format()}",
          fontWeight = FontWeight.Bold,
          style = MaterialTheme.typography.labelSmall,
          color = themeColor,
          maxLines = 1,
        )
      }
      tag?.let { tag ->
        if (score != null) {
          Text(
            text = " • ",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelSmall,
            color = tagHintColor,
            maxLines = 1,
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
          color = themeColor,
          maxLines = 1,
        )
      }
      timestamp?.let { timestamp ->
        if (score != null || tag != null) {
          Text(
            text = " • ",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = ContentAlphas.Medium),
            maxLines = 1,
          )
        }
        val millis = timestamp.toEpochMilliseconds()
        Text(
          text =
            DateUtils.getRelativeTimeSpanString(
                millis,
                System.currentTimeMillis(),
                0L,
                DateUtils.FORMAT_ABBREV_ALL,
              )
              .toString(),
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurface.copy(alpha = ContentAlphas.Medium),
          maxLines = 1,
        )
      }
    }
  }
}

@Composable
private fun ItemFooter(item: CatchUpItem, modifier: Modifier = Modifier) {
  TextItemFooter(author = item.author, source = item.source, modifier = modifier)
}

@Composable
fun TextItemFooter(author: String?, source: String?, modifier: Modifier = Modifier) {
  val textColor = MaterialTheme.colorScheme.onSurface.copy(alpha = ContentAlphas.Medium)
  if (author != null || source != null) {
    Row(
      modifier = modifier,
      verticalAlignment = Alignment.Bottom,
      horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      // Author
      author
        ?.takeUnless { it.isBlank() }
        ?.let { author ->
          Text(
            text = author,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            maxLines = 1,
          )
        }
      // Source
      source
        ?.takeUnless { it.isBlank() }
        ?.let { source ->
          if (author != null) {
            Text(
              text = " — ",
              style = MaterialTheme.typography.labelSmall,
              color = textColor,
              maxLines = 1,
            )
          }
          Text(
            text = source,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            maxLines = 1,
          )
        }
    }
  }
}

fun LazyListScope.handleLoadStates(lazyItems: LazyPagingItems<CatchUpItem>, themeColor: Color) {
  lazyItems.apply {
    when {
      loadState.refresh is LoadState.Loading -> {
        item { LoadingView(themeColor, Modifier.fillParentMaxSize()) }
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
            ::retry,
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

@Preview(uiMode = UI_MODE_NIGHT_NO)
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun PreviewTextItem() {
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
            mark = Mark(text = "14"),
          ),
        themeColor = colorResource(R.color.colorAccent),
      )
    }
  }
}
