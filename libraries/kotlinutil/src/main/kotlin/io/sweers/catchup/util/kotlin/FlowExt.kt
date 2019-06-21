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

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect

internal object NULL
private class LocalAbortFlowException : CancellationException(
    "Flow was aborted, no more elements needed") {
  override fun fillInStackTrace(): Throwable {
    return this
  }
}

/**
 * A terminal operator that returns the first element emitted by the flow matching the given [predicate] and then cancels flow's collection.
 * Returns null if the flow has not contained elements matching the [predicate].
 */
@ExperimentalCoroutinesApi
suspend fun <T> Flow<T>.firstOrNull(predicate: suspend (T) -> Boolean): T? {
  var result: Any? = NULL
  try {
    collect { value ->
      if (predicate(value)) {
        result = value
        throw LocalAbortFlowException()
      }
    }
  } catch (e: LocalAbortFlowException) {
    // Do nothing
  }

  return if (result === NULL) {
    null
  } else {
    @Suppress("UNCHECKED_CAST")
    result as T
  }
}

/**
 * A terminal operator that returns `true` if at least one element emitted by the flow matches the
 * given [predicate]. If there's a match, it then cancels flow's collection. Returns `false` if no
 * elements matched the [predicate].
 */
@ExperimentalCoroutinesApi
suspend fun <T> Flow<T>.any(predicate: suspend (T) -> Boolean): Boolean {
  return firstOrNull(predicate) != null
}
