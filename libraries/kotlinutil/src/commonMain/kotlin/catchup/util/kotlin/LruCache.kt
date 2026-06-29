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
package catchup.util.kotlin

import catchup.util.kotlin.NullableLruCache.Optional.None
import catchup.util.kotlin.NullableLruCache.Optional.Some

class LruCache<K : Any, V : Any>(private val cacheSize: Int) :
  LinkedHashMap<K, V>(
    cacheSize,
    // NOTE using named arguments here breaks the compiler
    0.75f,
    true,
  ) {

  init {
    require(cacheSize >= 0)
  }

  override fun removeEldestEntry(eldest: Map.Entry<K, V>): Boolean {
    return size > cacheSize
  }
}

class NullableLruCache<K : Any, V : Any>(maxSize: Int) {
  @PublishedApi internal val delegate = LruCache<K, Optional<V>>(maxSize)

  inline fun computeIfAbsent(key: K, create: () -> V?): V? {
    return when (val cached = delegate[key]) {
      is Some -> cached.value
      is None -> null
      null -> create().also { delegate[key] = if (it == null) None else Some(it) }
    }
  }

  @PublishedApi
  internal sealed class Optional<out T> {
    data class Some<out T : Any>(val value: T) : Optional<T>()

    data object None : Optional<Nothing>()
  }
}
