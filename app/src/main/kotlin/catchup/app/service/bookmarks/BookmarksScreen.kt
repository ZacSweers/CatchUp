package catchup.app.service.bookmarks

import androidx.annotation.ColorInt
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.DismissDirection.EndToStart
import androidx.compose.material3.DismissDirection.StartToEnd
import androidx.compose.material3.DismissValue.Default
import androidx.compose.material3.DismissValue.DismissedToStart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismiss
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDismissState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import app.cash.sqldelight.paging3.QueryPagingSource
import catchup.app.data.LinkManager
import catchup.app.service.ClickableItem
import catchup.app.service.PlaceholderItem
import catchup.app.service.TextItem
import catchup.app.service.UrlMeta
import catchup.app.service.bookmarks.BookmarksScreen.Event.Click
import catchup.app.service.bookmarks.BookmarksScreen.Event.Remove
import catchup.base.ui.BackPressNavButton
import catchup.bookmarks.BookmarkRepository
import catchup.compose.rememberStableCoroutineScope
import catchup.deeplink.DeepLinkable
import catchup.di.AppScope
import catchup.service.api.CatchUpItem
import catchup.service.api.ServiceMeta
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import com.squareup.anvil.annotations.ContributesMultibinding
import dagger.multibindings.StringKey
import dev.zacsweers.catchup.R.string
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

/*
 * TODO
 *  - Export bookmarks
 */

@ContributesMultibinding(AppScope::class, boundType = DeepLinkable::class)
@StringKey("bookmarks")
@Parcelize
object BookmarksScreen : Screen, DeepLinkable {
  override fun createScreen(queryParams: ImmutableMap<String, List<String?>>) = BookmarksScreen

  data class State(
    val items: LazyPagingItems<CatchUpItem>,
    val serviceMetaMap: ImmutableMap<String, ServiceMeta>,
    val eventSink: (Event) -> Unit,
  ) : CircuitUiState

  sealed interface Event : CircuitUiEvent {
    data class Click(val url: String, @ColorInt val themeColor: Int) : Event

    data class Remove(val id: Long) : Event
  }
}

@CircuitInject(BookmarksScreen::class, AppScope::class)
@OptIn(ExperimentalPagingApi::class)
class BookmarksPresenter
@Inject
constructor(
  private val bookmarksRepository: BookmarkRepository,
  private val linkManager: LinkManager,
  private val serviceMetaMap: Map<String, ServiceMeta>,
) : Presenter<BookmarksScreen.State> {
  @Composable
  override fun present(): BookmarksScreen.State {
    val itemsFlow = remember {
      Pager(config = PagingConfig(pageSize = 50), initialKey = 0, remoteMediator = null) {
          QueryPagingSource(
            countQuery = bookmarksRepository.bookmarksCountQuery(),
            transacter = bookmarksRepository.bookmarksTransacter(),
            context = Dispatchers.IO,
            queryProvider = { limit, offset -> bookmarksRepository.bookmarksQuery(limit, offset) },
          )
        }
        .flow
    }
    val lazyItems = itemsFlow.collectAsLazyPagingItems()
    val metaMap = remember { serviceMetaMap.toImmutableMap() }
    val scope = rememberStableCoroutineScope()
    val context = LocalContext.current
    return BookmarksScreen.State(lazyItems, metaMap) { event ->
      when (event) {
        is Remove -> {
          bookmarksRepository.removeBookmark(event.id)
        }
        is Click -> {
          scope.launch {
            val meta = UrlMeta(event.url, event.themeColor, context)
            linkManager.openUrl(meta)
          }
        }
      }
    }
  }
}

@CircuitInject(BookmarksScreen::class, AppScope::class)
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun Bookmarks(state: BookmarksScreen.State, modifier: Modifier = Modifier) {
  Scaffold(
    modifier = modifier,
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    containerColor = Color.Transparent,
    topBar = {
      TopAppBar(
        title = { Text(stringResource(string.title_bookmarks)) },
        navigationIcon = { BackPressNavButton() },
      )
    },
  ) { innerPadding ->
    if (state.items.itemCount == 0) {
      Box(Modifier.padding(innerPadding).fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("No bookmarks", color = MaterialTheme.colorScheme.onBackground)
      }
    } else {
      BookmarksList(state, Modifier.padding(innerPadding))
    }
  }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun BookmarksList(state: BookmarksScreen.State, modifier: Modifier = Modifier) {
  LazyColumn(modifier.fillMaxHeight()) {
    items(
      count = state.items.itemCount,
      // Here we use the new itemKey extension on LazyPagingItems to
      // handle placeholders automatically, ensuring you only need to provide
      // keys for real items
      key = state.items.itemKey { it.id },
    ) { index ->
      val item = state.items[index]
      if (item == null) {
        PlaceholderItem(Color.Unspecified)
      } else {
        val dismissState = rememberDismissState(confirmValueChange = { it == DismissedToStart })

        if (dismissState.currentValue == DismissedToStart) {
          // TODO offer an undo option after a pause?
          // TODO no exit animation yet https://issuetracker.google.com/issues/150812265#comment30
          state.eventSink(Remove(item.id))
        }

        val serviceMeta = remember(item.serviceId) { state.serviceMetaMap[item.serviceId] }
        val themeColorRes = remember(serviceMeta) { serviceMeta?.themeColor }
        val themeColor = themeColorRes?.let { colorResource(it) } ?: Color.Unspecified
        // When swiping from start to end, we don't dismiss and instead use this to indicate
        // metadata about the bookmark, like the service it's from.
        SwipeToDismiss(
          modifier = Modifier.animateItemPlacement(),
          state = dismissState,
          background = {
            val color =
              when (dismissState.dismissDirection) {
                StartToEnd -> themeColor
                EndToStart -> MaterialTheme.colorScheme.error
                null -> Color.Unspecified
              }
            val alignment =
              when (dismissState.dismissDirection) {
                StartToEnd -> Alignment.CenterStart
                EndToStart -> Alignment.CenterEnd
                null -> Alignment.CenterStart
              }
            Box(
              modifier = Modifier.fillMaxSize().background(color),
              contentAlignment = alignment,
            ) {
              when (dismissState.dismissDirection) {
                StartToEnd -> {
                  serviceMeta?.let {
                    Icon(
                      imageVector = ImageVector.vectorResource(it.icon),
                      contentDescription = stringResource(it.name),
                      modifier = Modifier.padding(32.dp).size(32.dp),
                      tint = Color.White,
                    )
                  }
                }
                EndToStart ->
                  Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    modifier = Modifier.padding(32.dp).size(32.dp),
                    tint = MaterialTheme.colorScheme.onError
                  )
                null -> {
                  // Do nothing
                }
              }
            }
          },
          dismissContent = {
            val clickUrl = item.clickUrl
            if (clickUrl != null) {
              ClickableItem(
                modifier = Modifier.animateItemPlacement(),
                onClick = { state.eventSink(Click(clickUrl, themeColor.toArgb())) },
              ) {
                TextItem(item, themeColor)
              }
            } else {
              TextItem(
                item,
                themeColor,
                modifier = Modifier.animateItemPlacement(),
              )
            }
          }
        )
      }
    }
  }
}
