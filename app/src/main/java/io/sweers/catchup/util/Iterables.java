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

import java.util.List;

import static io.sweers.catchup.util.Preconditions.checkNotNull;

/**
 * Utils for Iterables
 */

public final class Iterables {

  private Iterables() {

  }

  /**
   * Returns the element at the specified position in an iterable.
   *
   * <p><b>{@code Stream} equivalent:</b> {@code stream.skip(position).findFirst().get()}
   * (throws {@code NoSuchElementException} if out of bounds)
   *
   * @param position position of the element to return
   * @return the element at the specified position in {@code iterable}
   * @throws IndexOutOfBoundsException if {@code position} is negative or
   * greater than or equal to the size of {@code iterable}
   */
  public static <T> T get(Iterable<T> iterable, int position) {
    checkNotNull(iterable);
    return (iterable instanceof List) ? ((List<T>) iterable).get(position)
        : Iterators.get(iterable.iterator(), position);
  }
}
