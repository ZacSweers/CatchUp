package io.sweers.catchup.ui.activity

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsControllerCompat
import coil.annotation.ExperimentalCoilApi
import coil.compose.AsyncImage
import coil.imageLoader
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.size.Precision
import coil.size.Scale
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.slack.circuit.backstack.NavDecoration
import com.slack.circuit.backstack.SaveableBackStack
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.foundation.NavigatorDefaults
import com.slack.circuit.foundation.screen
import com.slack.circuit.overlay.LocalOverlayHost
import com.slack.circuit.overlay.OverlayHost
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.Screen
import com.slack.circuit.runtime.presenter.Presenter
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dev.zacsweers.catchup.circuit.BottomSheetOverlay
import dev.zacsweers.catchup.compose.CatchUpTheme
import dev.zacsweers.catchup.compose.rememberStableCoroutineScope
import dev.zacsweers.catchup.di.AppScope
import io.sweers.catchup.R
import io.sweers.catchup.base.ui.NavButton
import io.sweers.catchup.base.ui.NavButtonType
import io.sweers.catchup.data.LinkManager
import io.sweers.catchup.service.api.UrlMeta
import io.sweers.catchup.ui.activity.FlickToDismissState.FlickGestureState.Dismissed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import me.saket.telephoto.subsamplingimage.ImageSource
import me.saket.telephoto.subsamplingimage.SubSamplingImage
import me.saket.telephoto.subsamplingimage.rememberSubSamplingImageState
import me.saket.telephoto.zoomable.ZoomableContentLocation
import me.saket.telephoto.zoomable.ZoomableViewport
import me.saket.telephoto.zoomable.ZoomableViewportState
import me.saket.telephoto.zoomable.graphicsLayer
import me.saket.telephoto.zoomable.rememberZoomableViewportState

@Parcelize
data class ImageViewerScreen(
  val id: String,
  val url: String,
  val isBitmap: Boolean,
  val alias: String?,
  val sourceUrl: String,
) : Screen {
  data class State(
    val id: String,
    val url: String,
    val alias: String?,
    val sourceUrl: String,
    val isBitmap: Boolean,
    val eventSink: (Event) -> Unit,
  ) : CircuitUiState

  sealed interface Event : CircuitUiEvent {
    object Close : Event
    object NoOp : Event // Weird but necessary because of the reuse in bottom sheet
    object ShareImage : Event
    object CopyImage : Event
    object SaveImage : Event
    data class OpenInBrowser(val url: String) : Event
  }
}

class ImageViewerPresenter
@AssistedInject
constructor(
  @Assisted private val screen: ImageViewerScreen,
  @Assisted private val navigator: Navigator,
  private val linkManager: LinkManager
) : Presenter<ImageViewerScreen.State> {
  @CircuitInject(ImageViewerScreen::class, AppScope::class)
  @AssistedFactory
  fun interface Factory {
    fun create(
      screen: ImageViewerScreen,
      navigator: Navigator,
    ): ImageViewerPresenter
  }

  @Composable
  override fun present(): ImageViewerScreen.State {
    val context = LocalContext.current
    val accentColor = colorResource(R.color.colorAccent).toArgb()
    val scope = rememberCoroutineScope()
    return ImageViewerScreen.State(
      id = screen.id,
      url = screen.url,
      alias = screen.alias,
      isBitmap = screen.isBitmap,
      sourceUrl = screen.sourceUrl,
    ) { event ->
      // TODO finish implementing these. Also why is copying an image on android so terrible in
      //  2023.
      when (event) {
        ImageViewerScreen.Event.Close -> navigator.pop()
        ImageViewerScreen.Event.CopyImage -> {}
        is ImageViewerScreen.Event.OpenInBrowser -> {
          scope.launch { linkManager.openUrl(UrlMeta(event.url, accentColor, context)) }
        }
        ImageViewerScreen.Event.SaveImage -> {}
        ImageViewerScreen.Event.ShareImage -> {}
        ImageViewerScreen.Event.NoOp -> {}
      }
    }
  }
}

@OptIn(ExperimentalCoilApi::class)
@CircuitInject(ImageViewerScreen::class, AppScope::class)
@Composable
fun ImageViewer(state: ImageViewerScreen.State, modifier: Modifier = Modifier) {
  val sink = state.eventSink
  var showChrome by remember { mutableStateOf(true) }
  val systemUiController = rememberSystemUiController()
  systemUiController.isSystemBarsVisible = showChrome
  DisposableEffect(systemUiController) {
    val originalSystemBarsBehavior = systemUiController.systemBarsBehavior
    // Set BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE so the UI doesn't jump when it hides
    systemUiController.systemBarsBehavior =
      WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    onDispose {
      // TODO this is too late for some reason
      systemUiController.isSystemBarsVisible = true
      systemUiController.systemBarsBehavior = originalSystemBarsBehavior
    }
  }

  val context = LocalContext.current
  val imageSource: ImageSource? by
    produceState<ImageSource?>(null) {
      if (state.isBitmap) {
        val url = state.url
        val result =
          context.imageLoader.execute(
            ImageRequest.Builder(context)
              .data(url)
              // In-memory caching will be handled by SubSamplingImage.
              .memoryCachePolicy(CachePolicy.DISABLED)
              .build()
          )
        if (result is SuccessResult) {
          value =
            ImageSource.stream { context ->
              val diskCache = context.imageLoader.diskCache!!
              diskCache.fileSystem.source(diskCache[result.diskCacheKey!!]!!.data)
            }
        } else {
          // TODO: handle errors here.
          error("Failed to load image: $url. Result: $result")
        }
      }
    }

  CatchUpTheme(useDarkTheme = true) {
    val backgroundAlpha: Float by
      animateFloatAsState(targetValue = 1f, animationSpec = tween(), label = "backgroundAlpha")
    Surface(
      modifier.fillMaxSize().animateContentSize(),
      color = Color.Black.copy(alpha = backgroundAlpha),
      contentColor = Color.White
    ) {
      Box(Modifier.fillMaxSize()) {
        // Image + scrim

        val dismissState = rememberFlickToDismissState()
        if (dismissState.gestureState is Dismissed) {
          sink(ImageViewerScreen.Event.Close)
        }
        // TODO bind scrim with flick. animate scrim out after flick finishes? Or with flick?
        FlickToDismiss(state = dismissState) {
          val overlayHost = LocalOverlayHost.current
          val scope = rememberStableCoroutineScope()
          val viewportState = rememberZoomableViewportState(maxZoomFactor = 2f)
          ZoomableViewport(
            state = viewportState,
            onClick = { showChrome = !showChrome },
            onLongClick = { launchShareSheet(scope, overlayHost, state, sink) },
            contentScale = ContentScale.Inside,
          ) {
            if (state.isBitmap) {
              Crossfade(targetState = imageSource, label = "Crossfade subsampled image") { source ->
                if (source == null) {
                  if (state.alias == null) {
                    // TODO show a loading indicator?
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                      CircularProgressIndicator()
                    }
                    return@Crossfade
                  } else {
                    PlaceholderImage(
                      context.imageLoader.memoryCache!![MemoryCache.Key(state.alias)]!!
                        .bitmap
                        .asImageBitmap(),
                      modifier = Modifier.fillMaxSize(),
                    )
                  }
                } else {
                  SubSamplingImage(
                    modifier = Modifier.fillMaxSize(),
                    state =
                      rememberSubSamplingImageState(
                        imageSource = source,
                        viewportState = viewportState,
                      ),
                    contentDescription = null,
                  )
                }
              }
            } else {
              NormalSizedRemoteImage(state.url, state.alias, viewportState)
            }
          }
        }

        // TODO pick color based on if image is underneath it or not. Similar to badges
        AnimatedVisibility(
          showChrome,
          enter = fadeIn(),
          exit = fadeOut(),
        ) {
          NavButton(
            Modifier.align(Alignment.TopStart).padding(16.dp).statusBarsPadding(),
            NavButtonType.CLOSE,
          ) {
            sink(ImageViewerScreen.Event.Close)
          }
        }
      }
    }
  }
}

private fun launchShareSheet(
  scope: CoroutineScope,
  overlayHost: OverlayHost,
  state: ImageViewerScreen.State,
  sink: (ImageViewerScreen.Event) -> Unit,
) =
  scope.launch {
    val result =
      overlayHost.show(
        BottomSheetOverlay<Unit, ImageViewerScreen.Event>(
          Unit,
          onDismiss = { ImageViewerScreen.Event.NoOp }
        ) { _, navigator ->
          Column {
            // TODO icons?
            Text(
              modifier =
                Modifier.fillMaxWidth()
                  .clickable { navigator.finish(ImageViewerScreen.Event.ShareImage) }
                  .padding(16.dp),
              text = "Share"
            )
            Text(
              modifier =
                Modifier.fillMaxWidth()
                  .clickable { navigator.finish(ImageViewerScreen.Event.SaveImage) }
                  .padding(16.dp),
              text = "Save"
            )
            Text(
              modifier =
                Modifier.fillMaxWidth()
                  .clickable { navigator.finish(ImageViewerScreen.Event.CopyImage) }
                  .padding(16.dp),
              text = "Copy"
            )
            Text(
              modifier =
                Modifier.fillMaxWidth()
                  .clickable { navigator.finish(ImageViewerScreen.Event.OpenInBrowser(state.url)) }
                  .padding(16.dp),
              text = "Open in Browser"
            )
          }
        }
      )
    sink(result)
  }

@Composable
private fun NormalSizedRemoteImage(
  url: String,
  alias: String?,
  viewportState: ZoomableViewportState,
  modifier: Modifier = Modifier
) {
  AsyncImage(
    modifier = modifier.graphicsLayer(viewportState.contentTransformation).fillMaxSize(),
    model =
      ImageRequest.Builder(LocalContext.current)
        .data(url)
        .precision(Precision.EXACT)
        .scale(Scale.FIT)
        .apply { alias?.let(this::placeholderMemoryCacheKey) }
        .build(),
    contentDescription = "The image",
    onState = {
      viewportState.setContentLocation(
        ZoomableContentLocation.fitInsideAndCenterAligned(it.painter?.intrinsicSize)
      )
    }
  )
}

@Composable
private fun PlaceholderImage(bitmap: ImageBitmap, modifier: Modifier = Modifier) {
  Image(
    bitmap = bitmap,
    modifier = modifier,
    contentDescription = "The image",
  )
}

// TODO
//  generalize this when there's a factory pattern for it in Circuit
//  shared element transitions?
class ImageViewerAwareNavDecoration : NavDecoration {
  @Composable
  override fun <T> DecoratedContent(
    arg: T,
    backStackDepth: Int,
    modifier: Modifier,
    content: @Composable (T) -> Unit
  ) {
    val decoration =
      if (
        arg is Pair<*, *> &&
          arg.first is SaveableBackStack.Record &&
          (arg.first as SaveableBackStack.Record).screen is ImageViewerScreen
      ) {
        NavigatorDefaults.EmptyDecoration
      } else {
        NavigatorDefaults.DefaultDecoration
      }
    decoration.DecoratedContent(arg, backStackDepth, modifier, content)
  }
}
