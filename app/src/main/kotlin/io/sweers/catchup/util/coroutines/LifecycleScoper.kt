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

import android.os.Looper
import androidx.lifecycle.GenericLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Scope suspending work to run when [LifecycleOwner] is at least at state [whenAtLeast].
 * Runs [begin] each time [LifecycleOwner] reaches the target state and cancels the scope
 * whenever it falls beneath the target state again. Useful for connections and subscriptions.
 *
 * Adapted from https://gist.github.com/adamp/c6d213af7d931b0f00e9ca396d57dacd
 */
fun LifecycleOwner.liveCoroutineScope(
    whenAtLeast: Lifecycle.State = Lifecycle.State.STARTED,
    begin: suspend CoroutineScope.() -> Unit
) {
  val scoper = LifecycleScoper(whenAtLeast, begin)
  if (Looper.myLooper() == Looper.getMainLooper()) {
    lifecycle.addObserver(scoper)
  } else {
    GlobalScope.launch(Dispatchers.Main) {
      lifecycle.addObserver(scoper)
    }
  }
}

private class LifecycleScoper(
    whenAtLeast: Lifecycle.State,
    private val begin: suspend CoroutineScope.() -> Unit
) : GenericLifecycleObserver {

  private val upEvent = eventUpTo(whenAtLeast)
  private val downEvent = eventDownFrom(whenAtLeast)

  private var scope: CoroutineScope? = null

  override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
    if (event == upEvent) {
      val scope = newScope()
      this.scope = scope
      scope.launch(start = CoroutineStart.UNDISPATCHED) {
        begin()
      }
    } else if (event == downEvent) {
      scope?.apply {
        coroutineContext.cancel()
        scope = null
      }
    }
  }

  private fun newScope() = CoroutineScope(Dispatchers.Main + SupervisorJob())

  private fun eventUpTo(state: Lifecycle.State): Lifecycle.Event = when (state) {
    Lifecycle.State.CREATED -> Lifecycle.Event.ON_CREATE
    Lifecycle.State.STARTED -> Lifecycle.Event.ON_START
    Lifecycle.State.RESUMED -> Lifecycle.Event.ON_RESUME
    else -> throw IllegalArgumentException("no valid up event to reach $state")
  }

  private fun eventDownFrom(state: Lifecycle.State): Lifecycle.Event = when (state) {
    Lifecycle.State.CREATED -> Lifecycle.Event.ON_DESTROY
    Lifecycle.State.STARTED -> Lifecycle.Event.ON_STOP
    Lifecycle.State.RESUMED -> Lifecycle.Event.ON_PAUSE
    else -> throw IllegalArgumentException("no valid down event from $state")
  }
}
