package dev.zacsweers.catchup.compose

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.debugInspectorInfo
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.BUFFERED
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

@Stable
interface ScrollToTop {
  val scrollToTopFlow: Flow<Unit>
}

class MutableScrollToTop : ScrollToTop {
  private val channel = Channel<Unit>(BUFFERED)

  suspend fun emit() {
    channel.send(Unit)
  }

  override val scrollToTopFlow: Flow<Unit>
    get() = channel.receiveAsFlow()
}

/**
 * A [CompositionLocal] that provides a [ScrollToTop] instance.
 *
 * This is intended to be used for cases where a top-level tab layout or
 */
val LocalScrollToTop = compositionLocalOf<ScrollToTop?> { null }

fun Modifier.scrollToTop(state: LazyListState) =
  composed(
    inspectorInfo =
      debugInspectorInfo {
        name = "scrollToTop"
        value = state
      }
  ) {
    val scrollToTop = LocalScrollToTop.current
    if (scrollToTop != null) {
      LaunchedEffect(Unit) {
        scrollToTop.scrollToTopFlow.collect {
          // If more than 50 items down, just jump without animation
          if (state.firstVisibleItemIndex > 50) {
            state.scrollToItem(0)
          } else {
            state.animateScrollToItem(0)
          }
        }
      }
    }
    Modifier
  }
