/*
 * Copyright (C) 2007 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.sweers.catchup.util

import com.google.errorprone.annotations.CanIgnoreReturnValue

/*
 * Utils for iterators.
 */

/**
 * Advances `iterator` `position + 1` times, returning the
 * element at the `position`th position.
 *
 * @param position position of the element to return
 * @return the element at the specified position in `iterator`
 * @throws IndexOutOfBoundsException if `position` is negative or
 * greater than or equal to the number of elements remaining in `iterator`
 */
operator fun <T> Iterator<T>.get(position: Int): T {
  val skipped = Iterators.advance(this, position)
  if (!hasNext()) {
    throw IndexOutOfBoundsException(
        "position ($position) must be less than the number of elements that remained ($skipped)")
  }
  return next()
}

object Iterators {

  /**
   * Calls [Iterator.next] on `iterator`, either [numberToAdvance] times
   * or until [Iterator.hasNext] returns `false`, whichever comes first.
   *
   * @return the number of elements the iterator was advanced
   */
  @CanIgnoreReturnValue
  fun advance(iterator: Iterator<*>, numberToAdvance: Int): Int {
    var i = 0
    while (i < numberToAdvance && iterator.hasNext()) {
      iterator.next()
      i++
    }
    return i
  }

  operator fun <E> Collection<E>.get(element: E): Int {
    return asSequence().indexOfFirst { it == element }
  }
}
