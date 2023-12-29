package catchup.util

import java.util.concurrent.LinkedBlockingDeque
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/** A simple concurrent ring buffer implementation. */
// TODO if this ever moves to KMP, need to find an alternative to LinkedBlockingDeque
class RingBuffer<T>(private val capacity: Int) {
  private val deque = LinkedBlockingDeque<T>(capacity)

  val size: Int
    get() = deque.size

  fun push(item: T) {
    synchronized(deque) {
      if (deque.size == capacity) {
        deque.removeFirst()
      }
      deque.addLast(item)
    }
  }

  fun clear() = deque.clear()

  fun toImmutableList(): ImmutableList<T> = deque.toImmutableList()
}
