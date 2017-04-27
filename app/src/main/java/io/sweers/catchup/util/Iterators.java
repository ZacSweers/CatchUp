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

package io.sweers.catchup.util;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.Iterator;

import static io.sweers.catchup.util.Preconditions.checkNotNull;

/**
 * Utils for iterators.
 */
public final class Iterators {

  private Iterators() {

  }

  /**
   * Advances {@code iterator} {@code position + 1} times, returning the
   * element at the {@code position}th position.
   *
   * @param position position of the element to return
   * @return the element at the specified position in {@code iterator}
   * @throws IndexOutOfBoundsException if {@code position} is negative or
   * greater than or equal to the number of elements remaining in
   * {@code iterator}
   */
  public static <T> T get(Iterator<T> iterator, int position) {
    int skipped = advance(iterator, position);
    if (!iterator.hasNext()) {
      throw new IndexOutOfBoundsException("position ("
          + position
          + ") must be less than the number of elements that remained ("
          + skipped
          + ")");
    }
    return iterator.next();
  }

  /**
   * Calls {@code next()} on {@code iterator}, either {@code numberToAdvance} times
   * or until {@code hasNext()} returns {@code false}, whichever comes first.
   *
   * @return the number of elements the iterator was advanced
   * @since 13.0 (since 3.0 as {@code Iterators.skip})
   */
  @CanIgnoreReturnValue public static int advance(Iterator<?> iterator, int numberToAdvance) {
    checkNotNull(iterator);
    int i;
    for (i = 0; i < numberToAdvance && iterator.hasNext(); i++) {
      iterator.next();
    }
    return i;
  }
}
