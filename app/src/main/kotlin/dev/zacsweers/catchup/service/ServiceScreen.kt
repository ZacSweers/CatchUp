package dev.zacsweers.catchup.service

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.items
import com.slack.circuit.CircuitCompositionLocals
import com.slack.circuit.CircuitConfig
import com.slack.circuit.CircuitContent
import com.slack.circuit.CircuitUiEvent
import com.slack.circuit.CircuitUiState
import com.slack.circuit.Presenter
import com.slack.circuit.Screen
import com.slack.circuit.codegen.annotations.CircuitInject
import com.squareup.anvil.annotations.ContributesMultibinding
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dev.zacsweers.catchup.compose.CatchUpTheme
import dev.zacsweers.catchup.di.AppScope
import dev.zacsweers.catchup.di.android.FragmentKey
import dev.zacsweers.catchup.service.ServiceScreen.State.TextState
import dev.zacsweers.catchup.service.ServiceScreen.State.VisualState
import io.sweers.catchup.R
import io.sweers.catchup.base.ui.BaseFragment
import io.sweers.catchup.data.LinkManager
import io.sweers.catchup.databinding.ComposeViewBinding
import io.sweers.catchup.service.api.CatchUpItem
import io.sweers.catchup.service.api.ImageViewerData
import io.sweers.catchup.service.api.Service
import io.sweers.catchup.service.api.UrlMeta
import io.sweers.catchup.service.api.VisualService
import io.sweers.catchup.ui.activity.FinalServices
import javax.inject.Inject
import javax.inject.Provider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

private const val ARG_SERVICE_KEY = "service_key"

// TODO implement caching in paging
@FragmentKey(ServiceFragment2::class)
@ContributesMultibinding(AppScope::class, boundType = Fragment::class)
class ServiceFragment2 @Inject constructor(private val circuitConfig: CircuitConfig) :
  BaseFragment<ComposeViewBinding>() {

  override val bindingInflater: (LayoutInflater, ViewGroup?, Boolean) -> ComposeViewBinding
    get() = ComposeViewBinding::inflate

  fun forService(serviceKey: String) = apply {
    arguments = Bundle().apply { putString(ARG_SERVICE_KEY, serviceKey) }
  }

  private val serviceKey: String
    get() = requireArguments().getString(ARG_SERVICE_KEY)!!

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    val composeView = binding.root
    composeView.setContent {
      CatchUpTheme {
        CircuitCompositionLocals(circuitConfig) { CircuitContent(ServiceScreen(serviceKey)) }
      }
    }
  }
}

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
      val spanConfig: VisualService.SpanConfig,
      override val eventSink: (Event) -> Unit
    ) : State
  }

  sealed interface Event : CircuitUiEvent {
    data class ItemClicked(val item: CatchUpItem) : Event
    data class MarkClicked(val item: CatchUpItem) : Event
  }
}

// TODO implement on scroll to top callbacks
class ServicePresenter
@AssistedInject
constructor(
  @Assisted private val screen: ServiceScreen,
  private val linkManager: LinkManager,
  @FinalServices private val services: @JvmSuppressWildcards Map<String, Provider<Service>>,
) : Presenter<ServiceScreen.State> {
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
    val themeColorInt = context.getColor(service.meta().themeColor)
    val themeColor = Color(themeColorInt)
    val items: Flow<PagingData<CatchUpItem>> = remember {
      // TODO
      //  preference page size
      //  retain pager or even the flow?
      Pager(PagingConfig(pageSize = 50)) { ServiceSource(service) }.flow
    }
    val coroutineScope = rememberCoroutineScope()
    val eventSink: (ServiceScreen.Event) -> Unit = { event ->
      when (event) {
        is ServiceScreen.Event.ItemClicked -> {
          val imageData =
            if (service.meta().isVisual) {
              val info = event.item.imageInfo!!
              ImageViewerData(
                id = info.imageId,
                imageUrl = info.detailUrl,
                cacheKey = info.cacheKey,
                sourceUrl = info.sourceUrl,
                image = null
              )
            } else {
              null
            }
          val url = event.item.clickUrl
          coroutineScope.launch {
            linkManager.openUrl(UrlMeta(url, themeColorInt, context, imageData))
          }
        }
        is ServiceScreen.Event.MarkClicked -> {
          val url = event.item.markClickUrl
          coroutineScope.launch { linkManager.openUrl(UrlMeta(url, themeColorInt, context, null)) }
        }
      }
    }
    return when (service.meta().isVisual) {
      true ->
        VisualState(
          items = items,
          themeColor = themeColor,
          spanConfig = (service as VisualService).spanConfig(),
          eventSink = eventSink
        )
      false -> TextState(items = items, themeColor = themeColor, eventSink = eventSink)
    }
  }

  @CircuitInject(ServiceScreen::class, AppScope::class)
  @AssistedFactory
  fun interface Factory {
    fun create(screen: ServiceScreen): ServicePresenter
  }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@CircuitInject(ServiceScreen::class, AppScope::class)
@Composable
fun Service(state: ServiceScreen.State) {
  val eventSink = state.eventSink
  val lazyItems: LazyPagingItems<CatchUpItem> = state.items.collectAsLazyPagingItems()
  var refreshing by remember { mutableStateOf(false) }
  val pullRefreshState = rememberPullRefreshState(refreshing, onRefresh = lazyItems::refresh)
  // TODO this isn't accounting for actual system bars ugh
  Box(Modifier.pullRefresh(pullRefreshState).systemBarsPadding()) {
    if (state is VisualState) {
      VisualServiceUi(lazyItems, state.themeColor, { refreshing = it }, state.spanConfig, eventSink)
    } else {
      TextServiceUi(lazyItems, state.themeColor, { refreshing = it }, eventSink)
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
fun ErrorItem(text: String, modifier: Modifier = Modifier, onRetryClick: () -> Unit) {
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
        Modifier.size(64.dp).clickable(
          interactionSource = remember { MutableInteractionSource() },
          indication = rememberRipple(bounded = false)
        ) {
          atEnd = !atEnd
        }
    )
    Text(text)
    ElevatedButton(onClick = onRetryClick) { Text("Retry") }
  }
}

@Composable
fun LoadingView(themeColor: Color, modifier: Modifier = Modifier) {
  Column(
    modifier = modifier,
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    CircularProgressIndicator(color = themeColor)
  }
}

@Composable
fun LoadingItem() {
  CircularProgressIndicator(
    modifier =
      Modifier.fillMaxWidth().padding(16.dp).wrapContentWidth(Alignment.CenterHorizontally),
    color = MaterialTheme.colorScheme.outline
  )
}
