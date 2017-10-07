/*
 * Copyright (c) 2017 Zac Sweers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.sweers.catchup.util.collect

import android.os.Build.VERSION_CODES
import android.support.annotation.RequiresApi
import android.text.TextUtils
import java.util.function.Consumer

/**
 * A List implementation that emits toString() calls as comma separated values, useful for
 * something like multi-get requests in retrofit.
 *
 * @param <E> the element type
 */
class CommaJoinerList<E> constructor(private val delegate: List<E>) : List<E> {

  override val size: Int
    get() = delegate.size

  override fun isEmpty() = delegate.isEmpty()

  override fun contains(element: E) = delegate.contains(element)

  override fun iterator() = delegate.iterator()

  override fun get(index: Int) = delegate[index]

  @RequiresApi(VERSION_CODES.N)
  override fun forEach(action: Consumer<in E>?) {
    delegate.forEach(action)
  }

  @RequiresApi(VERSION_CODES.N)
  override fun stream() = delegate.stream()

  override fun indexOf(element: E) = delegate.indexOf(element)

  override fun equals(other: Any?) = delegate == other

  @RequiresApi(VERSION_CODES.N)
  override fun parallelStream() = delegate.parallelStream()

  @RequiresApi(VERSION_CODES.N)
  override fun spliterator() = delegate.spliterator()

  override fun containsAll(elements: Collection<E>) = delegate.containsAll(elements)

  override fun lastIndexOf(element: E) = delegate.lastIndexOf(element)

  override fun hashCode() = delegate.hashCode()

  override fun listIterator() = delegate.listIterator()

  override fun listIterator(index: Int) = delegate.listIterator(index)

  override fun subList(fromIndex: Int, toIndex: Int) = delegate.subList(fromIndex, toIndex)

  override fun toString(): String = TextUtils.join(",", this)
}

fun <T : Any> List<T>.toCommaJoinerList() = CommaJoinerList(this)
