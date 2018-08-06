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

package io.sweers.catchup.ui.base

import com.uber.autodispose.lifecycle.CorrespondingEventsFunction
import com.uber.autodispose.lifecycle.LifecycleEndedException

/**
 * Activity lifecycle events.
 */
enum class ActivityEvent {
  CREATE, START, RESUME, PAUSE, STOP, DESTROY;

  companion object {
    val LIFECYCLE = CorrespondingEventsFunction { lastEvent: ActivityEvent ->
      return@CorrespondingEventsFunction when (lastEvent) {
        CREATE -> DESTROY
        START -> STOP
        RESUME -> PAUSE
        PAUSE -> STOP
        STOP -> DESTROY
        DESTROY -> throw LifecycleEndedException(
            "Cannot bind to Activity lifecycle after it's been destroyed.")
      }
    }
  }
}
