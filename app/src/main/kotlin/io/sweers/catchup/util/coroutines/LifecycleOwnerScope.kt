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

package io.sweers.catchup.util.coroutines

import androidx.annotation.MainThread
import androidx.lifecycle.GenericLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlin.coroutines.CoroutineContext

/**
 * Create a [CoroutineScope] for [owner] that will be cancelled when [owner] is destroyed.
 *
 * Adapted from https://gist.github.com/adamp/c6d213af7d931b0f00e9ca396d57dacd
 */
@MainThread
fun LifecycleOwner.lifecycleCoroutineScope(): CoroutineScope = LifecycleOwnerScope().also(
    lifecycle::addObserver)

private class LifecycleOwnerScope : CoroutineScope, GenericLifecycleObserver {
  override val coroutineContext: CoroutineContext = Dispatchers.Main + SupervisorJob()

  override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
    if (event == Lifecycle.Event.ON_DESTROY) {
      coroutineContext.cancel()
    }
  }
}
