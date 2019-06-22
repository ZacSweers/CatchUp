/*
 * Copyright (C) 2019. Zac Sweers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
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
package io.sweers.catchup.util.kotlin

/**
 * Provides ring buffer implementation.
 *
 * Buffer overflow is not allowed so [add] doesn't overwrite tail but raises an exception.
 */
internal class RingBuffer<T>(val capacity: Int) : AbstractList<T>(), RandomAccess {
  init {
    require(capacity >= 0) { "ring buffer capacity should not be negative but it is $capacity" }
  }

  private val buffer = arrayOfNulls<Any?>(capacity)
  private var startIndex: Int = 0

  override var size: Int = 0
    private set

  override fun get(index: Int): T {
    if (index < 0 || index >= size) {
      throw IndexOutOfBoundsException("index: $index, size: $size")
    }
    @Suppress("UNCHECKED_CAST")
    return buffer[startIndex.forward(index)] as T
  }

  fun isFull() = size == capacity

  override fun iterator(): Iterator<T> = object : AbstractIterator<T>() {
    private var count = size
    private var index = startIndex

    override fun computeNext() {
      if (count == 0) {
        done()
      } else {
        @Suppress("UNCHECKED_CAST")
        setNext(buffer[index] as T)
        index = index.forward(1)
        count--
      }
    }
  }

  @Suppress("UNCHECKED_CAST")
  override fun <T> toArray(array: Array<T>): Array<T> {
    val result: Array<T?> =
        if (array.size < this.size) array.copyOf(this.size) else array as Array<T?>

    val size = this.size

    var widx = 0
    var idx = startIndex

    while (widx < size && idx < capacity) {
      result[widx] = buffer[idx] as T
      widx++
      idx++
    }

    idx = 0
    while (widx < size) {
      result[widx] = buffer[idx] as T
      widx++
      idx++
    }
    if (result.size > this.size) result[this.size] = null

    return result as Array<T>
  }

  override fun toArray(): Array<Any?> {
    return toArray(arrayOfNulls(size))
  }

  /**
   * Add [element] to the buffer or fail with [IllegalStateException] if no free space available in the buffer
   */
  fun add(element: T) {
    if (isFull()) {
      throw IllegalStateException("ring buffer is full")
    }

    buffer[startIndex.forward(size)] = element
    size++
  }

  /**
   * Removes [n] first elements from the buffer or fails with [IllegalArgumentException] if not enough elements in the buffer to remove
   */
  fun removeFirst(n: Int) {
    require(n >= 0) { "n shouldn't be negative but it is $n" }
    require(n <= size) { "n shouldn't be greater than the buffer size: n = $n, size = $size" }

    if (n > 0) {
      val start = startIndex
      val end = start.forward(n)

      if (start > end) {
        buffer.fill(null, start, capacity)
        buffer.fill(null, 0, end)
      } else {
        buffer.fill(null, start, end)
      }

      startIndex = end
      size -= n
    }
  }

  @Suppress("NOTHING_TO_INLINE")
  private inline fun Int.forward(n: Int): Int = (this + n) % capacity

  // TODO: replace with Array.fill from stdlib when available in common
  private fun <T> Array<T>.fill(element: T, fromIndex: Int = 0, toIndex: Int = size) {
    for (idx in fromIndex until toIndex) {
      this[idx] = element
    }
  }
}
