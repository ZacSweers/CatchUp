/*
 * Copyright (c) 2018 Zac Sweers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@file:Suppress("unused")

package io.sweers.catchup.util

import timber.log.Timber

/*
 * Adapted from [Slimber](https://github.com/PaulWoitaschek/Slimber/blob/bea76b32563906edc8cf196ce4b6cfce8d12d6e6/slimber/src/main/kotlin/de/paul_woitaschek/slimber/Slimber.kt)
 */

/** Invokes an action if any trees are planted */
inline fun ifPlanted(action: () -> Unit) {
  if (Timber.treeCount() != 0) {
    action()
  }
}

/** Delegates the provided message to [Timber.e] if any trees are planted. */
inline fun e(message: () -> String) = ifPlanted { Timber.e(message()) }

/** Delegates the provided message to [Timber.e] if any trees are planted. */
inline fun e(throwable: Throwable, message: () -> String) = ifPlanted {
  Timber.e(throwable, message())
}

/** Delegates the provided message to [Timber.w] if any trees are planted. */
inline fun w(message: () -> String) = ifPlanted { Timber.w(message()) }

/** Delegates the provided message to [Timber.w] if any trees are planted. */
inline fun w(throwable: Throwable, message: () -> String) = ifPlanted {
  Timber.w(throwable, message())
}

/** Delegates the provided message to [Timber.i] if any trees are planted. */
inline fun i(message: () -> String) = ifPlanted { Timber.i(message()) }

/** Delegates the provided message to [Timber.i] if any trees are planted. */
inline fun i(throwable: Throwable, message: () -> String) = ifPlanted {
  Timber.i(throwable, message())
}

/** Delegates the provided message to [Timber.d] if any trees are planted. */
inline fun d(message: () -> String) = ifPlanted { Timber.d(message()) }

/** Delegates the provided message to [Timber.d] if any trees are planted. */
inline fun d(throwable: Throwable, message: () -> String) = ifPlanted {
  Timber.d(throwable, message())
}

/** Delegates the provided message to [Timber.v] if any trees are planted. */
inline fun v(message: () -> String) = ifPlanted { Timber.v(message()) }

/** Delegates the provided message to [Timber.v] if any trees are planted. */
inline fun v(throwable: Throwable, message: () -> String) = ifPlanted {
  Timber.v(throwable, message())
}

/** Delegates the provided message to [Timber.wtf] if any trees are planted. */
inline fun wtf(message: () -> String) = ifPlanted { Timber.wtf(message()) }

/** Delegates the provided message to [Timber.wtf] if any trees are planted. */
inline fun wtf(throwable: Throwable, message: () -> String) = ifPlanted {
  Timber.wtf(throwable, message())
}

/** Delegates the provided message to [Timber.log] if any trees are planted. */
inline fun log(priority: Int, t: Throwable, message: () -> String) = ifPlanted {
  Timber.log(priority, t, message())
}

/** Delegates the provided message to [Timber.log] if any trees are planted. */
inline fun log(priority: Int, message: () -> String) = ifPlanted {
  Timber.log(priority, message())
}

