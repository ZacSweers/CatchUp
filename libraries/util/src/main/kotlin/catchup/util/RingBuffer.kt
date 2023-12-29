package catchup.util

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/** A simple ring buffer implementation. */
class RingBuffer<T>(private val capacity: Int) {
  private val deque: ArrayDeque<T> = ArrayDeque(capacity)

  val size: Int
    get() = deque.size

  fun push(item: T) {
    if (deque.size == capacity) {
      deque.removeFirst()
    }
    deque.addLast(item)
  }

  fun pop(): T? = if (deque.isNotEmpty()) deque.removeFirst() else null

  fun peek(): T? = deque.firstOrNull()

  fun clear() {
    deque.clear()
  }

  fun toImmutableList(): ImmutableList<T> = deque.toImmutableList()
}
