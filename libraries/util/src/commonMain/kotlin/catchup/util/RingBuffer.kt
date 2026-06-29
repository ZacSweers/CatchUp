/*
 * Copyright (C) 2026. Zac Sweers
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
