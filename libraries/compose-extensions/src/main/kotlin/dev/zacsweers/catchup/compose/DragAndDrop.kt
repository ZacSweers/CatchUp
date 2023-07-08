package dev.zacsweers.catchup.compose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/*
 * Copied from https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/foundation/foundation/integration-tests/foundation-demos/src/main/java/androidx/compose/foundation/demos/LazyColumnDragAndDropDemo.kt
 */

@Composable
fun rememberDragDropState(lazyListState: LazyListState, onMove: (Int, Int) -> Unit): DragDropState {
  val scope = rememberCoroutineScope()
  val state =
    remember(lazyListState) { DragDropState(state = lazyListState, onMove = onMove, scope = scope) }
  LaunchedEffect(state) {
    while (true) {
      val diff = state.scrollChannel.receive()
      lazyListState.scrollBy(diff)
    }
  }
  return state
}

class DragDropState
internal constructor(
  private val state: LazyListState,
  private val scope: CoroutineScope,
  private val onMove: (Int, Int) -> Unit
) {
  var draggingItemIndex by mutableStateOf<Int?>(null)
    private set

  internal val scrollChannel = Channel<Float>()

  private var draggingItemDraggedDelta by mutableStateOf(0f)
  private var draggingItemInitialOffset by mutableStateOf(0)
  internal val draggingItemOffset: Float
    get() =
      draggingItemLayoutInfo?.let { item ->
        draggingItemInitialOffset + draggingItemDraggedDelta - item.offset
      }
        ?: 0f

  private val draggingItemLayoutInfo: LazyListItemInfo?
    get() = state.layoutInfo.visibleItemsInfo.firstOrNull { it.index == draggingItemIndex }

  internal var previousIndexOfDraggedItem by mutableStateOf<Int?>(null)
    private set

  internal var previousItemOffset = Animatable(0f)
    private set

  internal fun onDragStart(offset: Offset) {
    state.layoutInfo.visibleItemsInfo
      .firstOrNull { item -> offset.y.toInt() in item.offset..(item.offset + item.size) }
      ?.also {
        draggingItemIndex = it.index
        draggingItemInitialOffset = it.offset
      }
  }

  internal fun onDragInterrupted() {
    if (draggingItemIndex != null) {
      previousIndexOfDraggedItem = draggingItemIndex
      val startOffset = draggingItemOffset
      scope.launch {
        previousItemOffset.snapTo(startOffset)
        previousItemOffset.animateTo(
          0f,
          spring(stiffness = Spring.StiffnessMediumLow, visibilityThreshold = 1f)
        )
        previousIndexOfDraggedItem = null
      }
    }
    draggingItemDraggedDelta = 0f
    draggingItemIndex = null
    draggingItemInitialOffset = 0
  }

  internal fun onDrag(offset: Offset) {
    draggingItemDraggedDelta += offset.y

    val draggingItem = draggingItemLayoutInfo ?: return
    val startOffset = draggingItem.offset + draggingItemOffset
    val endOffset = startOffset + draggingItem.size
    val middleOffset = startOffset + (endOffset - startOffset) / 2f

    val targetItem =
      state.layoutInfo.visibleItemsInfo.find { item ->
        middleOffset.toInt() in item.offset..item.offsetEnd && draggingItem.index != item.index
      }
    if (targetItem != null) {
      val scrollToIndex =
        if (targetItem.index == state.firstVisibleItemIndex) {
          draggingItem.index
        } else if (draggingItem.index == state.firstVisibleItemIndex) {
          targetItem.index
        } else {
          null
        }
      if (scrollToIndex != null) {
        scope.launch {
          // this is needed to neutralize automatic keeping the first item first.
          state.scrollToItem(scrollToIndex, state.firstVisibleItemScrollOffset)
          onMove.invoke(draggingItem.index, targetItem.index)
        }
      } else {
        onMove.invoke(draggingItem.index, targetItem.index)
      }
      draggingItemIndex = targetItem.index
    } else {
      val overscroll =
        when {
          draggingItemDraggedDelta > 0 ->
            (endOffset - state.layoutInfo.viewportEndOffset).coerceAtLeast(0f)
          draggingItemDraggedDelta < 0 ->
            (startOffset - state.layoutInfo.viewportStartOffset).coerceAtMost(0f)
          else -> 0f
        }
      if (overscroll != 0f) {
        scrollChannel.trySend(overscroll)
      }
    }
  }

  private val LazyListItemInfo.offsetEnd: Int
    get() = this.offset + this.size
}

fun Modifier.dragContainer(
  dragDropState: DragDropState,
  hapticFeedback: HapticFeedback? = null,
): Modifier {
  return pointerInput(dragDropState) {
    detectDragGesturesAfterLongPress(
      onDrag = { change, offset ->
        change.consume()
        dragDropState.onDrag(offset = offset)
      },
      onDragStart = { offset ->
        hapticFeedback?.performHapticFeedback(HapticFeedbackType.LongPress)
        dragDropState.onDragStart(offset)
      },
      onDragEnd = { dragDropState.onDragInterrupted() },
      onDragCancel = { dragDropState.onDragInterrupted() }
    )
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LazyItemScope.DraggableItem(
  dragDropState: DragDropState,
  index: Int,
  modifier: Modifier = Modifier,
  content: @Composable ColumnScope.(isDragging: Boolean) -> Unit
) {
  val dragging = index == dragDropState.draggingItemIndex
  val draggingModifier =
    if (dragging) {
      Modifier.zIndex(1f).graphicsLayer { translationY = dragDropState.draggingItemOffset }
    } else if (index == dragDropState.previousIndexOfDraggedItem) {
      Modifier.zIndex(1f).graphicsLayer { translationY = dragDropState.previousItemOffset.value }
    } else {
      Modifier.animateItemPlacement()
    }
  Column(modifier = modifier.then(draggingModifier)) { content(dragging) }
}
