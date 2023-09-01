package dev.zacsweers.catchup.service

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.map
import app.cash.sqldelight.paging3.QueryPagingSource
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.overlay.LocalOverlayHost
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuitx.android.IntentScreen
import com.slack.circuitx.overlays.showFullScreenOverlay
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dev.zacsweers.catchup.compose.dynamicAwareColor
import dev.zacsweers.catchup.di.AppScope
import dev.zacsweers.catchup.pullrefresh.PullRefreshIndicator
import dev.zacsweers.catchup.pullrefresh.pullRefresh
import dev.zacsweers.catchup.pullrefresh.rememberPullRefreshState
import dev.zacsweers.catchup.service.ServiceScreen.Event.ItemActionClicked.Action.FAVORITE
import dev.zacsweers.catchup.service.ServiceScreen.Event.ItemActionClicked.Action.SHARE
import dev.zacsweers.catchup.service.ServiceScreen.Event.ItemActionClicked.Action.SUMMARIZE
import dev.zacsweers.catchup.service.ServiceScreen.State.TextState
import dev.zacsweers.catchup.service.ServiceScreen.State.VisualState
import dev.zacsweers.catchup.summarizer.SummarizerScreen
import io.sweers.catchup.R
import io.sweers.catchup.data.LinkManager
import io.sweers.catchup.service.api.CatchUpItem
import io.sweers.catchup.service.api.ContentType
import io.sweers.catchup.service.api.Service
import io.sweers.catchup.service.api.UrlMeta
import io.sweers.catchup.ui.activity.ImageViewerScreen
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
    val items: Flow<PagingData<CatchUpItem>>
    val themeColor: Color
    val eventSink: (Event) -> Unit

    data class TextState(
      override val items: Flow<PagingData<CatchUpItem>>,
      override val themeColor: Color,
      override val eventSink: (Event) -> Unit
    ) : State

    data class VisualState(
      override val items: Flow<PagingData<CatchUpItem>>,
      override val themeColor: Color,
      override val eventSink: (Event) -> Unit
    ) : State
  }

  sealed interface Event : CircuitUiEvent {
    data class ItemClicked(val item: CatchUpItem) : Event

    data class ItemActionClicked(val item: CatchUpItem, val action: Action) : Event {
      enum class Action {
        FAVORITE,
        SHARE,
        SUMMARIZE
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
  private val catchUpDatabase: CatchUpDatabase,
  private val serviceMediatorFactory: ServiceMediator.Factory
) : Presenter<ServiceScreen.State> {
  @OptIn(ExperimentalPagingApi::class)
  @Composable
  override fun present(): ServiceScreen.State {
    val service = remember {
      services[screen.serviceKey]?.get()
        ?: throw IllegalArgumentException(
          "No service provided for ${screen.serviceKey}! Available are ${services.keys}"
        )
    }

    // TODO this is a bad pattern in circuit
    val context = LocalContext.current
    val themeColor =
      dynamicAwareColor(
        regularColor = { colorResource(service.meta().themeColor) },
        dynamicColor = { MaterialTheme.colorScheme.primary }
      )
    // TODO what's the right thing and scope to retain?
    // TODO this leaks the activity after destroy if using rememberRetained
    val pager = remember {
      // TODO
      //  preference page size
      //  retain pager or even the flow?
      Pager(
        config = PagingConfig(pageSize = 50),
        initialKey = service.meta().firstPageKey,
        remoteMediator = serviceMediatorFactory.create(service = service)
      ) {
        QueryPagingSource(
          countQuery = catchUpDatabase.serviceQueries.countItems(service.meta().id),
          transacter = catchUpDatabase.serviceQueries,
          context = Dispatchers.IO,
          queryProvider = { limit, offset ->
            catchUpDatabase.serviceQueries.itemsByService(service.meta().id, limit, offset)
          },
        )
      }
    }
    val items: Flow<PagingData<CatchUpItem>> =
      remember(pager) { pager.flow.map { data -> data.map { it.toCatchUpItem() } } }
    val coroutineScope = rememberCoroutineScope()
    val overlayHost = LocalOverlayHost.current
    val eventSink: (ServiceScreen.Event) -> Unit = { event ->
      when (event) {
        is ServiceScreen.Event.ItemClicked -> {
          coroutineScope.launch {
            if (service.meta().isVisual) {
              val info = event.item.imageInfo!!
              overlayHost.showFullScreenOverlay(
                ImageViewerScreen(
                  info.imageId,
                  info.detailUrl,
                  isBitmap = !info.animatable,
                  info.cacheKey,
                  info.sourceUrl
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
                    sourceUrl = url
                  )
                )
              } else {
                val meta = UrlMeta(url, themeColor.toArgb(), context)
                linkManager.openUrl(meta)
              }
            }
          }
        }
        is ServiceScreen.Event.ItemActionClicked -> {
          val url = event.item.clickUrl!!
          when (event.action) {
            FAVORITE -> {
              Toast.makeText(context, "Not implemented", Toast.LENGTH_SHORT).show()
            }
            SHARE -> {
              val shareIntent =
                Intent().apply {
                  action = Intent.ACTION_SEND
                  putExtra(Intent.EXTRA_TEXT, url)
                  type = "text/plain"
                }
              navigator.goTo(IntentScreen(Intent.createChooser(shareIntent, "Share")))
            }
            SUMMARIZE -> {
              coroutineScope.launch {
                overlayHost.showFullScreenOverlay(SummarizerScreen(event.item.title, url))
              }
            }
          }
        }
        is ServiceScreen.Event.MarkClicked -> {
          val url = event.item.markClickUrl
          coroutineScope.launch { linkManager.openUrl(UrlMeta(url, themeColor.toArgb(), context)) }
        }
      }
    }
    return when (service.meta().isVisual) {
      true -> VisualState(items = items, themeColor = themeColor, eventSink = eventSink)
      false -> TextState(items = items, themeColor = themeColor, eventSink = eventSink)
    }
  }

  @CircuitInject(ServiceScreen::class, AppScope::class)
  @AssistedFactory
  fun interface Factory {
    fun create(screen: ServiceScreen, navigator: Navigator): ServicePresenter
  }
}

@CircuitInject(ServiceScreen::class, AppScope::class)
@Composable
fun Service(state: ServiceScreen.State, modifier: Modifier = Modifier) {
  val lazyItems: LazyPagingItems<CatchUpItem> = state.items.collectAsLazyPagingItems()
  var refreshing by remember { mutableStateOf(false) }
  val pullRefreshState = rememberPullRefreshState(refreshing, onRefresh = lazyItems::refresh)
  Box(modifier.pullRefresh(pullRefreshState)) {
    if (state is VisualState) {
      VisualServiceUi(lazyItems, state.themeColor, { refreshing = it }, state.eventSink)
    } else {
      TextServiceUi(lazyItems, state.themeColor, { refreshing = it }, state.eventSink)
    }

    PullRefreshIndicator(
      refreshing = refreshing,
      state = pullRefreshState,
      contentColor = state.themeColor,
      modifier = Modifier.align(Alignment.TopCenter)
    )
  }
}

@OptIn(ExperimentalAnimationGraphicsApi::class)
@Composable
fun ErrorItem(text: String, modifier: Modifier = Modifier, onRetryClick: (() -> Unit)? = null) {
  Column(
    modifier = modifier.padding(16.dp),
    verticalArrangement = spacedBy(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally
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
          interactionSource = remember { MutableInteractionSource() },
          indication = rememberRipple(bounded = false)
        ) {
          atEnd = !atEnd
        }
    )
    Text(text, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface)
    onRetryClick?.let { ElevatedButton(onClick = it) { Text(stringResource(R.string.retry)) } }
  }
}

@Composable
fun LoadingView(themeColor: Color, modifier: Modifier = Modifier) {
  Box(
    modifier = modifier,
  ) {
    CircularProgressIndicator(color = themeColor, modifier = Modifier.align(Alignment.Center))
  }
}

@Composable
fun LoadingItem(modifier: Modifier = Modifier) {
  CircularProgressIndicator(
    modifier =
      modifier.fillMaxWidth().padding(16.dp).wrapContentWidth(Alignment.CenterHorizontally),
    color = MaterialTheme.colorScheme.outline
  )
}
