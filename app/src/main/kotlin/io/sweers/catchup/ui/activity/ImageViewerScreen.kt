package io.sweers.catchup.ui.activity

import androidx.annotation.ColorInt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import androidx.core.view.WindowInsetsControllerCompat
import coil.request.ImageRequest
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
import com.slack.circuitx.overlays.BottomSheetOverlay
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dev.zacsweers.catchup.compose.CatchUpTheme
import dev.zacsweers.catchup.compose.rememberStableCoroutineScope
import dev.zacsweers.catchup.di.AppScope
import io.sweers.catchup.R
import io.sweers.catchup.base.ui.NavButton
import io.sweers.catchup.base.ui.NavButtonType
import io.sweers.catchup.data.LinkManager
import io.sweers.catchup.service.api.UrlMeta
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import me.saket.telephoto.flick.FlickToDismiss
import me.saket.telephoto.flick.FlickToDismissState
import me.saket.telephoto.flick.FlickToDismissState.GestureState
import me.saket.telephoto.flick.FlickToDismissState.GestureState.Dismissing
import me.saket.telephoto.flick.rememberFlickToDismissState
import me.saket.telephoto.zoomable.ZoomSpec
import me.saket.telephoto.zoomable.coil.ZoomableAsyncImage
import me.saket.telephoto.zoomable.rememberZoomableImageState
import me.saket.telephoto.zoomable.rememberZoomableState

@Parcelize
data class ImageViewerScreen(
  val id: String,
  val url: String,
  val isBitmap: Boolean,
  val alias: String?,
  val sourceUrl: String,
  @ColorInt val backgroundColor: Int = Color.Unspecified.toArgb()
) : Screen {
  data class State(
    val id: String,
    val url: String,
    val alias: String?,
    val sourceUrl: String,
    val isBitmap: Boolean,
    val backgroundColor: Color,
    val eventSink: (Event) -> Unit,
  ) : CircuitUiState

  sealed interface Event : CircuitUiEvent {
    data object Close : Event

    data object NoOp : Event // Weird but necessary because of the reuse in bottom sheet

    data object ShareImage : Event

    data object CopyImage : Event

    data object SaveImage : Event

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
      backgroundColor = Color(screen.backgroundColor),
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

@CircuitInject(ImageViewerScreen::class, AppScope::class)
@Composable
fun ImageViewer(state: ImageViewerScreen.State, modifier: Modifier = Modifier) {
  var showChrome by remember { mutableStateOf(true) }
  val systemUiController = rememberSystemUiController()
  systemUiController.isSystemBarsVisible = showChrome
  DisposableEffect(systemUiController) {
    val originalSystemBarsBehavior = systemUiController.systemBarsBehavior
    // Set BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE so the UI doesn't jump when it hides
    systemUiController.systemBarsBehavior =
      WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    val originalIsDarkContent = systemUiController.systemBarsDarkContentEnabled
    systemUiController.systemBarsDarkContentEnabled = false
    onDispose {
      systemUiController.isSystemBarsVisible = true
      systemUiController.systemBarsBehavior = originalSystemBarsBehavior
      systemUiController.systemBarsDarkContentEnabled = originalIsDarkContent
    }
  }

  CatchUpTheme(useDarkTheme = true) {
    Scaffold(
      contentWindowInsets = WindowInsets(0, 0, 0, 0),
      containerColor = Color.Transparent,
      contentColor = Color.White,
    ) { contentPadding ->
      val flickState = rememberFlickToDismissState(rotateOnDrag = false)
      val backgroundColor =
        if (state.backgroundColor == Color.Unspecified) {
          MaterialTheme.colorScheme.background
        } else {
          state.backgroundColor
        }
      Box(
        modifier
          .padding(contentPadding)
          .fillMaxSize()
          .background(backgroundColor.copy(alpha = 1f - flickState.offsetFraction))
      ) {
        // Image + scrim
        CloseScreenOnFlickDismissEffect(flickState) {
          state.eventSink(ImageViewerScreen.Event.Close)
        }

        // TODO
        //  corner shape on drag offset
        //  dropshadow on dragging
        FlickToDismiss(state = flickState) {
          val overlayHost = LocalOverlayHost.current
          val scope = rememberStableCoroutineScope()
          val zoomableState = rememberZoomableState(ZoomSpec(maxZoomFactor = 2f))
          val imageState = rememberZoomableImageState(zoomableState)
          // TODO loading loading indicator if there's no memory cached alias

          // TODO elevation and corner radius don't actually work currently
          val targetElevation =
            if (flickState.gestureState is GestureState.Dragging) {
              32.dp
            } else {
              0.dp
            }
          val targetRadius =
            if (flickState.gestureState is GestureState.Dragging) {
              32.dp
            } else {
              0.dp
            }

          val elevation by animateDpAsState(targetElevation, label = "Animated elevation")
          val cornerRadius by animateDpAsState(targetRadius, label = "Animated corner radius")

          // Scale x/y based on the flick offset
          // We scale at most to 0.95f, and lerp this within the first 30% of the offset fraction
          val scale by remember {
            derivedStateOf {
              val adjustedFraction =
                when {
                  flickState.offsetFraction <= 0f -> 0f
                  flickState.offsetFraction >= 0.3f -> 1f
                  else -> flickState.offsetFraction / 0.3f
                }
              lerp(1f, 0.95f, adjustedFraction)
            }
          }
          ZoomableAsyncImage(
            model =
              ImageRequest.Builder(LocalContext.current)
                .data(state.url)
                .apply { state.alias?.let(::placeholderMemoryCacheKey) }
                .build(),
            contentDescription = "TODO",
            modifier =
              Modifier.fillMaxSize()
                .graphicsLayer(scaleX = scale, scaleY = scale)
                .zIndex(elevation.value)
                .clip(RoundedCornerShape(cornerRadius)),
            state = imageState,
            onClick = { showChrome = !showChrome },
            onLongClick = { launchShareSheet(scope, overlayHost, state) },
          )
        }

        // TODO pick color based on if image is underneath it or not. Similar to badges?
        //  Alternatively make this just a very small button?
        AnimatedVisibility(
          showChrome && flickState.gestureState == GestureState.Idle,
          enter = fadeIn(),
          exit = fadeOut(),
        ) {
          NavButton(
            Modifier.align(Alignment.TopStart).padding(16.dp).statusBarsPadding(),
            NavButtonType.CLOSE,
          ) {
            state.eventSink(ImageViewerScreen.Event.Close)
          }
        }
      }
    }
  }
}

@Composable
private fun CloseScreenOnFlickDismissEffect(
  flickState: FlickToDismissState,
  onDismiss: () -> Unit
) {
  val gestureState = flickState.gestureState

  if (gestureState is Dismissing) {
    LaunchedEffect(Unit) {
      delay(gestureState.animationDuration / 2)
      onDismiss()
    }
  }
}

private fun launchShareSheet(
  scope: CoroutineScope,
  overlayHost: OverlayHost,
  state: ImageViewerScreen.State,
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
    state.eventSink(result)
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
