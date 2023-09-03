package catchup.compose

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
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

@Composable
fun ScrollToTopHandler(state: LazyListState, animateLimit: Int = 25) {
  val wrapper = remember {
    object : StateWrapper {
      override val firstVisibleItemIndex: Int
        get() = state.firstVisibleItemIndex

      override suspend fun scrollToItem(index: Int) {
        state.scrollToItem(index)
      }

      override suspend fun animateScrollToItem(index: Int) {
        state.animateScrollToItem(index)
      }
    }
  }
  ScrollToTopHandler(wrapper, animateLimit)
}

@Composable
fun ScrollToTopHandler(state: LazyStaggeredGridState, animateLimit: Int = 50) {
  val wrapper = remember {
    object : StateWrapper {
      override val firstVisibleItemIndex: Int
        get() = state.firstVisibleItemIndex

      override suspend fun scrollToItem(index: Int) {
        state.scrollToItem(index)
      }

      override suspend fun animateScrollToItem(index: Int) {
        state.animateScrollToItem(index)
      }
    }
  }
  ScrollToTopHandler(wrapper, animateLimit)
}

@Composable
private fun ScrollToTopHandler(state: StateWrapper, animateLimit: Int) {
  val scrollToTop = LocalScrollToTop.current
  if (scrollToTop != null) {
    LaunchedEffect(Unit) {
      scrollToTop.scrollToTopFlow.collect {
        // If more than animateLimit items down, just jump without animation because scroll to takes
        // too long
        if (state.firstVisibleItemIndex > animateLimit) {
          state.scrollToItem(0)
        } else {
          state.animateScrollToItem(0)
        }
      }
    }
  }
}

// Because the different Lazy*States don't share a common interface
@Stable
private interface StateWrapper {
  val firstVisibleItemIndex: Int

  suspend fun scrollToItem(index: Int)

  suspend fun animateScrollToItem(index: Int)
}
