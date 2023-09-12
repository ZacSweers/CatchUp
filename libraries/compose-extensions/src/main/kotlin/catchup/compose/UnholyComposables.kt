package catchup.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

/**
 * Returns the previous value of [current].
 *
 * Adapted from http://www.billjings.net/posts/title/the-unholy-composable/?up=technical
 */
@Composable
fun <R, T : R> previous(current: T, initial: R): R {
  val lastValue = remember { mutableStateOf(initial) }
  return remember(current) {
    val previous = lastValue.value
    lastValue.value = current
    previous
  }
}
