package io.sweers.catchup.ui.activity

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Precision
import coil.size.Scale
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.slack.circuit.CircuitUiState
import com.slack.circuit.Navigator
import com.slack.circuit.NavigatorDefaults
import com.slack.circuit.Presenter
import com.slack.circuit.Screen
import com.slack.circuit.backstack.NavDecoration
import com.slack.circuit.backstack.SaveableBackStack
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.screen
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import de.mr_pine.zoomables.ZoomableImage
import de.mr_pine.zoomables.rememberZoomableState
import dev.zacsweers.catchup.compose.CatchUpTheme
import dev.zacsweers.catchup.di.AppScope
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
  ) : CircuitUiState
}

class ImageViewerPresenter
@AssistedInject
constructor(
  @Assisted private val screen: ImageViewerScreen,
  @Assisted private val navigator: Navigator,
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
    return ImageViewerScreen.State(
      id = screen.id,
      url = screen.url,
      alias = screen.alias,
      sourceUrl = screen.sourceUrl,
    )
  }
}

@CircuitInject(ImageViewerScreen::class, AppScope::class)
@Composable
fun ImageViewer(state: ImageViewerScreen.State) {
  val systemUiController = rememberSystemUiController()
  // TODO why is this animation so slooooooooow
  systemUiController.isSystemBarsVisible = false
  DisposableEffect(Unit) { onDispose { systemUiController.isSystemBarsVisible = true } }
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
      Modifier.fillMaxSize(),
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
                state.alias?.let(this::memoryCacheKey)
                precision(Precision.EXACT)
                scale(Scale.FILL)

                // Don't cache this to avoid pushing other bitmaps out of the cache.
                memoryCachePolicy(CachePolicy.READ_ONLY)

                // Crossfade in the higher res version when it arrives
                // ...hopefully
                crossfade(true)
              }
              .build()
          )

        // TODO
        //  flick dismiss
        //  tap to show chrome
        ZoomableImage(
          coroutineScope = rememberCoroutineScope(),
          zoomableState = rememberZoomableState(),
          painter = painter,
          contentDescription = "The image",
          modifier = Modifier.fillMaxSize(),
        )

        IconButton(
          modifier = Modifier.align(Alignment.TopStart).padding(16.dp).statusBarsPadding(),
          onClick = { TODO() }
        ) {
          Icon(Icons.Filled.Close, contentDescription = "Close")
        }

        // TODO Options. Bottom sheet or bottom row?
        //  share, save, copy, open in browser
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
