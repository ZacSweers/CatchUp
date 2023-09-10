package catchup.app.service.bookmarks

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.DismissDirection.EndToStart
import androidx.compose.material3.DismissDirection.StartToEnd
import androidx.compose.material3.DismissValue.Default
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import app.cash.sqldelight.paging3.QueryPagingSource
import catchup.app.service.ClickableItem
import catchup.app.service.PlaceholderItem
import catchup.app.service.TextItem
import catchup.app.service.bookmarks.BookmarksScreen.Event.Remove
import catchup.base.ui.BackPressNavButton
import catchup.bookmarks.BookmarkRepository
import catchup.deeplink.DeepLinkable
import catchup.di.AppScope
import catchup.service.api.CatchUpItem
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.parcelize.Parcelize

@ContributesMultibinding(AppScope::class, boundType = DeepLinkable::class)
@StringKey("bookmarks")
@Parcelize
object BookmarksScreen : Screen, DeepLinkable {
  override fun createScreen(queryParams: ImmutableMap<String, List<String?>>) = BookmarksScreen

  data class State(val items: Flow<PagingData<CatchUpItem>>, val eventSink: (Event) -> Unit) :
    CircuitUiState

  sealed interface Event : CircuitUiEvent {
    data class Remove(val id: Long) : Event
  }
}

@CircuitInject(BookmarksScreen::class, AppScope::class)
@OptIn(ExperimentalPagingApi::class)
class BookmarksPresenter @Inject constructor(private val bookmarksRepository: BookmarkRepository) :
  Presenter<BookmarksScreen.State> {
  @Composable
  override fun present(): BookmarksScreen.State {
    val pager = remember {
      // TODO
      //  retain pager or even the flow?
      Pager(config = PagingConfig(pageSize = 50), initialKey = 0, remoteMediator = null) {
        QueryPagingSource(
          countQuery = bookmarksRepository.bookmarksCountQuery(),
          transacter = bookmarksRepository.bookmarksTransacter(),
          context = Dispatchers.IO,
          queryProvider = { limit, offset -> bookmarksRepository.bookmarksQuery(limit, offset) },
        )
      }
    }
    val items: Flow<PagingData<CatchUpItem>> = remember(pager) { pager.flow }
    return BookmarksScreen.State(items) { event ->
      when (event) {
        is Remove -> bookmarksRepository.removeBookmark(event.id)
      }
    }
  }
}

@CircuitInject(BookmarksScreen::class, AppScope::class)
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun Bookmarks(state: BookmarksScreen.State, modifier: Modifier = Modifier) {
  val lazyItems: LazyPagingItems<CatchUpItem> = state.items.collectAsLazyPagingItems()
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
  ) {
    // TODO empty state
    LazyColumn(modifier = Modifier.padding(it)) {
      items(
        count = lazyItems.itemCount,
        // Here we use the new itemKey extension on LazyPagingItems to
        // handle placeholders automatically, ensuring you only need to provide
        // keys for real items
        key = lazyItems.itemKey { it.id },
      ) { index ->
        val item = lazyItems[index]
        if (item == null) {
          PlaceholderItem(Color.Unspecified)
        } else {
          val dismissState =
            rememberDismissState(
              confirmValueChange = {
                if (it != Default) {
                  state.eventSink(Remove(item.id))
                  // TODO this is a jarring animation to do immediately
                  // TODO offer an undo option
                  lazyItems.refresh()
                }
                true
              }
            )
          SwipeToDismiss(
            state = dismissState,
            background = {
              val alignment =
                when (dismissState.dismissDirection) {
                  StartToEnd -> Alignment.CenterStart
                  EndToStart -> Alignment.CenterEnd
                  null -> Alignment.CenterStart
                }
              Box(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.error),
                contentAlignment = alignment,
              ) {
                Icon(
                  imageVector = Icons.Default.Delete,
                  contentDescription = "Delete",
                  modifier = Modifier.padding(32.dp).size(32.dp),
                  tint = MaterialTheme.colorScheme.onError
                )
              }
            },
            dismissContent = {
              // TODO where's the elevation on press/drag?
              ClickableItem(
                modifier = Modifier.animateItemPlacement(),
                onClick = { /* TODO */},
              ) {
                Column(Modifier.animateContentSize()) { TextItem(item, Color.Unspecified) }
              }
            }
          )
        }
      }
    }
  }
}
