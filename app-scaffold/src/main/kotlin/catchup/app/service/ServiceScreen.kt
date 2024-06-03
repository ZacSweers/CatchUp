package catchup.app.service

import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadState
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.map
import app.cash.sqldelight.paging3.QueryPagingSource
import catchup.app.CatchUpPreferences
import catchup.app.data.LinkManager
import catchup.app.service.ServiceScreen.Event
import catchup.app.service.ServiceScreen.Event.ItemActionClicked
import catchup.app.service.ServiceScreen.Event.ItemActionClicked.Action.SHARE
import catchup.app.service.ServiceScreen.Event.ItemActionClicked.Action.SUMMARIZE
import catchup.app.service.ServiceScreen.Event.ItemClicked
import catchup.app.service.ServiceScreen.Event.MarkClicked
import catchup.app.service.ServiceScreen.State
import catchup.app.service.ServiceScreen.State.TextState
import catchup.app.service.ServiceScreen.State.VisualState
import catchup.app.service.detail.ServiceDetailScreen
import catchup.app.ui.activity.ImageViewerScreen
import catchup.base.ui.rememberEventSink
import catchup.compose.dynamicAwareColor
import catchup.compose.rememberRetainedCoroutineScope
import catchup.compose.rememberStableCoroutineScope
import catchup.di.AppScope
import catchup.di.ContextualFactory
import catchup.di.DataMode
import catchup.di.DataMode.OFFLINE
import catchup.service.api.CatchUpItem
import catchup.service.api.ContentType
import catchup.service.api.Service
import catchup.service.api.toCatchUpItem
import catchup.service.db.CatchUpDatabase
import catchup.summarizer.SummarizerScreen
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.overlay.LocalOverlayHost
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuitx.overlays.showFullScreenOverlay
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dev.zacsweers.catchup.app.scaffold.R
import javax.inject.Provider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import okhttp3.HttpUrl.Companion.toHttpUrl

@Parcelize
data class ServiceScreen(val serviceKey: String) : Screen {
  sealed interface State : CircuitUiState {
    val items: LazyPagingItems<CatchUpItem>
    val themeColor: Color
    val eventSink: (Event) -> Unit

    data class TextState(
      override val items: LazyPagingItems<CatchUpItem>,
      override val themeColor: Color,
      override val eventSink: (Event) -> Unit,
    ) : State

    data class VisualState(
      override val items: LazyPagingItems<CatchUpItem>,
      override val themeColor: Color,
      override val eventSink: (Event) -> Unit,
    ) : State
  }

  sealed interface Event : CircuitUiEvent {
    data class ItemClicked(val item: CatchUpItem) : Event

    data class ItemActionClicked(val item: CatchUpItem, val action: Action) : Event {
      enum class Action {
        SHARE,
        SUMMARIZE,
      }
    }

    data class MarkClicked(val item: CatchUpItem) : Event
  }
}

class ServicePresenter
@AssistedInject
constructor(
  @Assisted private val screen: ServiceScreen,
  @Assisted private val navigator: Navigator,
  private val linkManager: LinkManager,
  private val services: @JvmSuppressWildcards Map<String, Provider<Service>>,
  private val dbFactory: ContextualFactory<DataMode, out CatchUpDatabase>,
  private val serviceMediatorFactory: ServiceMediator.Factory,
  private val catchUpPreferences: CatchUpPreferences,
) : Presenter<State> {
  @Composable
  override fun present(): State {
    val service =
      remember(screen.serviceKey) {
        services[screen.serviceKey]?.get()
          ?: throw IllegalArgumentException(
            "No service provided for ${screen.serviceKey}! Available are ${services.keys}"
          )
      }

    val themeColor =
      dynamicAwareColor(
        regularColor = { colorResource(service.meta().themeColor) },
        dynamicColor = { MaterialTheme.colorScheme.primary },
      )

    val scope = rememberStableCoroutineScope()
    val overlayHost = LocalOverlayHost.current
    val eventSink: (Event) -> Unit = rememberEventSink { event ->
      when (event) {
        is ItemClicked -> {
          // TODO what's the best way to handle these
          if (event.item.detailKey != null) {
            navigator.goTo(
              ServiceDetailScreen(
                serviceId = event.item.serviceId!!,
                itemId = event.item.id,
                id = event.item.detailKey!!,
                title = event.item.title,
                // TODO we should be able to put this in an item if available
                text = "",
                imageUrl =
                  if (event.item.contentType == ContentType.IMAGE) event.item.clickUrl else null,
                // TODO this needs to be conditional. For example, we don't want selftext links
                //  here. Maybe a new external link property?
                linkUrl = event.item.clickUrl,
                score = event.item.score?.second?.toLong() ?: 0L,
                commentsCount = event.item.mark?.text?.toIntOrNull() ?: 0,
                tag = event.item.tag,
                author = event.item.author,
                timestamp = event.item.timestamp?.toEpochMilliseconds(),
              )
            )
          } else {
            scope.launch {
              if (service.meta().isVisual) {
                val info = event.item.imageInfo!!
                overlayHost.showFullScreenOverlay(
                  ImageViewerScreen(
                    info.imageId,
                    info.detailUrl,
                    isBitmap = !info.animatable,
                    info.cacheKey,
                    info.sourceUrl,
                  )
                )
              } else {
                val url = event.item.clickUrl!!
                if (event.item.contentType == ContentType.IMAGE) {
                  // TODO generalize this
                  val bestGuessIsBitmap =
                    url.toHttpUrl().pathSegments.last().let { path ->
                      path.endsWith(".jpg", ignoreCase = true) ||
                        path.endsWith(".png", ignoreCase = true) ||
                        path.endsWith(".gif", ignoreCase = true)
                    }
                  overlayHost.showFullScreenOverlay(
                    ImageViewerScreen(
                      id = url,
                      url = url,
                      isBitmap = bestGuessIsBitmap,
                      alias = null,
                      sourceUrl = url,
                    )
                  )
                } else {
                  linkManager.openUrl(url, themeColor)
                }
              }
            }
          }
        }
        is ItemActionClicked -> {
          val url = event.item.clickUrl!!
          when (event.action) {
            SHARE -> {
              linkManager.shareUrl(url, event.item.title)
            }
            SUMMARIZE -> {
              scope.launch {
                overlayHost.showFullScreenOverlay(SummarizerScreen(event.item.title, url))
              }
            }
          }
        }
        is MarkClicked -> {
          event.item.markClickUrl?.let { url ->
            scope.launch { linkManager.openUrl(url, themeColor) }
          }
        }
      }
    }

    val dataMode by catchUpPreferences.dataMode.collectAsState()
    val pagingScope = rememberRetainedCoroutineScope()

    // Changes to DataMode in settings will trigger a restart, but not bad to key explicitly here
    // too
    // We use Paging's `cachedIn` operator with our retained CoroutineScope
    val items =
      rememberRetained(dataMode) {
          // TODO
          //  preference page size
          createPager(service, dataMode, 20).cachedIn(pagingScope)
        }
        .collectAsLazyPagingItems()
    return when (service.meta().isVisual) {
      true -> VisualState(items = items, themeColor = themeColor, eventSink = eventSink)
      false -> TextState(items = items, themeColor = themeColor, eventSink = eventSink)
    }
  }

  @OptIn(ExperimentalPagingApi::class)
  private fun createPager(
    service: Service,
    dataMode: DataMode,
    pageSize: Int,
  ): Flow<PagingData<CatchUpItem>> {
    // TODO make DB factory based on data modes
    val db by lazy { dbFactory.create(dataMode) }
    val remoteMediator =
      if (dataMode == OFFLINE) {
        null
      } else {
        serviceMediatorFactory.create(service, db)
      }

    return Pager(
        config = PagingConfig(pageSize = pageSize),
        initialKey = service.meta().firstPageKey,
        remoteMediator = remoteMediator,
      ) {
        // Real data driven through the DB
        // If we're in fake mode, we'll get a fake DB
        QueryPagingSource(
          countQuery = db.serviceQueries.countItems(service.meta().id),
          transacter = db.serviceQueries,
          context = Dispatchers.IO,
          queryProvider = { limit, offset ->
            db.serviceQueries.itemsByService(service.meta().id, limit, offset)
          },
        )
      }
      .flow
      .map { data -> data.map { it.toCatchUpItem() } }
  }

  @CircuitInject(ServiceScreen::class, AppScope::class)
  @AssistedFactory
  fun interface Factory {
    fun create(screen: ServiceScreen, navigator: Navigator): ServicePresenter
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@CircuitInject(ServiceScreen::class, AppScope::class)
@Composable
fun Service(state: State, modifier: Modifier = Modifier) {
  val refreshLoadState by rememberUpdatedState(state.items.loadState.refresh)
  val pullRefreshState = rememberPullToRefreshState()
  PullToRefreshBox(
    modifier = modifier,
    isRefreshing = refreshLoadState == LoadState.Loading,
    state = pullRefreshState,
    onRefresh = state.items::refresh,
    indicator = {
      PullToRefreshDefaults.Indicator(
        state = pullRefreshState,
        isRefreshing = refreshLoadState == LoadState.Loading,
        color = state.themeColor,
        modifier = Modifier.align(Alignment.TopCenter),
      )
    },
  ) {
    if (state is VisualState) {
      VisualServiceUi(state.items, state.themeColor, state.eventSink)
    } else {
      TextServiceUi(state.items, state.themeColor, state.eventSink)
    }
  }
}

@OptIn(ExperimentalAnimationGraphicsApi::class)
@Composable
fun ErrorItem(text: String, modifier: Modifier = Modifier, onRetryClick: (() -> Unit)? = null) {
  Column(
    modifier = modifier.padding(16.dp),
    verticalArrangement = spacedBy(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    val image = AnimatedImageVector.animatedVectorResource(R.drawable.avd_no_connection)
    var atEnd by remember { mutableStateOf(false) }
    // autoplay the AVD
    DisposableEffect(Unit) {
      atEnd = !atEnd
      onDispose {}
    }
    Image(
      painter = rememberAnimatedVectorPainter(animatedImageVector = image, atEnd = atEnd),
      contentDescription = "No connection",
      modifier =
        Modifier.size(72.dp).clickable(
          interactionSource = null,
          indication = ripple(bounded = false),
        ) {
          atEnd = !atEnd
        },
    )
    Text(text, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface)
    onRetryClick?.let { ElevatedButton(onClick = it) { Text(stringResource(R.string.retry)) } }
  }
}

@Composable
fun LoadingView(themeColor: Color, modifier: Modifier = Modifier) {
  Box(modifier = modifier) {
    CircularProgressIndicator(color = themeColor, modifier = Modifier.align(Alignment.Center))
  }
}

@Composable
fun LoadingItem(modifier: Modifier = Modifier) {
  Box(modifier = modifier.fillMaxWidth().padding(16.dp)) {
    CircularProgressIndicator(
      modifier = Modifier.align(Alignment.Center),
      color = MaterialTheme.colorScheme.outline,
    )
  }
}
