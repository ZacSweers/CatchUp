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
package catchup.util.kotlin

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalForInheritanceCoroutinesApi
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge

@OptIn(DelicateCoroutinesApi::class)
fun <E> SendChannel<E>.safeOffer(value: E) =
  !isClosedForSend &&
    try {
      trySend(value).isSuccess
    } catch (t: Throwable) {
      // Ignore all
      false
    }

suspend fun <V, K> Flow<V>.groupBy(selector: (V) -> K): Flow<Pair<K, List<V>>> = flow {
  val map = mutableMapOf<K, MutableList<V>>()
  collect { map.getOrPut(selector(it), ::mutableListOf).add(it) }
  map.entries.forEach { (key, value) -> emit(key to value) }
}

suspend fun <T, K : Comparable<K>> Flow<T>.sortBy(selector: (T) -> K): Flow<T> = flow {
  val list = mutableListOf<T>()
  collect { list.add(it) }
  list.sortBy(selector)
  list.forEach { emit(it) }
}

/**
 * A terminal operator that returns `true` if at least one element emitted by the flow matches the
 * given [predicate]. If there's a match, it then cancels flow's collection. Returns `false` if no
 * elements matched the [predicate].
 */
suspend fun <T> Flow<T>.any(predicate: suspend (T) -> Boolean): Boolean {
  return firstOrNull(predicate) != null
}

fun <T> Flow<T>.mergeWith(other: Flow<T>): Flow<T> = merge(this, other)

@OptIn(ExperimentalForInheritanceCoroutinesApi::class)
internal class MappedStateFlow<T, R>(
  private val source: StateFlow<T>,
  private val mapper: (T) -> R,
) : StateFlow<R> {

  override val value: R
    get() = mapper(source.value)

  override val replayCache: List<R>
    get() = source.replayCache.map(mapper)

  override suspend fun collect(collector: FlowCollector<R>): Nothing {
    source.collect { value -> collector.emit(mapper(value)) }
  }
}

// Because Coroutines still doesn't offer an API for this
// https://github.com/Kotlin/kotlinx.coroutines/issues/2514#issuecomment-775001647
fun <T, R> StateFlow<T>.mapToStateFlow(mapper: (T) -> R): StateFlow<R> =
  MappedStateFlow(this, mapper)
