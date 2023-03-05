package io.sweers.catchup.ui.activity

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Surface
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsControllerCompat
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.size.Precision
import coil.size.Scale
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.slack.circuit.CircuitUiEvent
import com.slack.circuit.CircuitUiState
import com.slack.circuit.Navigator
import com.slack.circuit.NavigatorDefaults
import com.slack.circuit.Presenter
import com.slack.circuit.Screen
import com.slack.circuit.backstack.NavDecoration
import com.slack.circuit.backstack.SaveableBackStack
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.overlay.LocalOverlayHost
import com.slack.circuit.screen
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import de.mr_pine.zoomables.ZoomableImage
import de.mr_pine.zoomables.rememberZoomableState
import dev.zacsweers.catchup.circuit.BottomSheetOverlay
import dev.zacsweers.catchup.compose.CatchUpTheme
import dev.zacsweers.catchup.di.AppScope
import io.sweers.catchup.R
import io.sweers.catchup.base.ui.NavButton
import io.sweers.catchup.base.ui.NavButtonType
import io.sweers.catchup.data.LinkManager
import io.sweers.catchup.service.api.UrlMeta
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

@Parcelize
data class ImageViewerScreen(
  val id: String,
  val url: String,
  val alias: String?,
  val sourceUrl: String,
) : Screen {
  data class State(
    val id: String,
    val url: String,
    val alias: String?,
    val sourceUrl: String,
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
  CatchUpTheme(useDarkTheme = true) {
    val backgroundAlpha: Float by
      animateFloatAsState(
        targetValue = 1f,
        animationSpec =
          tween(
            durationMillis = 200,
            easing = LinearEasing,
          )
      )
    Surface(
      modifier.fillMaxSize().animateContentSize(),
      color = Color.Black.copy(alpha = backgroundAlpha),
      contentColor = Color.White
    ) {
      Box(Modifier.fillMaxSize()) {
        // Image + scrim
        val painter =
          rememberAsyncImagePainter(
            ImageRequest.Builder(LocalContext.current)
              .data(state.url)
              .apply {
                state.alias?.let(this::placeholderMemoryCacheKey)
                precision(Precision.EXACT)
                scale(Scale.FILL)

                // Crossfade in the higher res version when it arrives
                // ...hopefully
                crossfade(true)
              }
              .build()
          )

        // TODO
        //  flick dismiss
        //  double tap conflicts with our own detectTapGesturesBelow
        //  if zooming too far out, animate back to 100%
        //  if rotated, rotate back after letting go
        val overlayHost = LocalOverlayHost.current
        val scope = rememberCoroutineScope()
        ZoomableImage(
          coroutineScope = scope,
          zoomableState = rememberZoomableState(),
          painter = painter,
          contentDescription = "The image",
          modifier =
            Modifier.fillMaxSize().pointerInput(Unit) {
              detectTapGestures(
                onTap = { showChrome = !showChrome },
                onLongPress = {
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
                                  .clickable {
                                    navigator.finish(ImageViewerScreen.Event.ShareImage)
                                  }
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
                                  .clickable {
                                    navigator.finish(
                                      ImageViewerScreen.Event.OpenInBrowser(state.url)
                                    )
                                  }
                                  .padding(16.dp),
                              text = "Open in Browser"
                            )
                          }
                        }
                      )
                    sink(result)
                  }
                }
              )
            },
        )

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
