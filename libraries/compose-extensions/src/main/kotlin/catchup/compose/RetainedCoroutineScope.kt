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
package catchup.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.RememberObserver
import com.slack.circuit.retained.rememberRetained
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel

/** https://chrisbanes.me/posts/retaining-beyond-viewmodels/ */
@Composable
fun rememberRetainedCoroutineScope(): CoroutineScope {
  return rememberRetained("coroutine_scope") {
      object : RememberObserver {
        val scope = CoroutineScope(Dispatchers.Main + Job())

        override fun onForgotten() {
          // We've been forgotten, cancel the CoroutineScope
          scope.cancel(null)
        }

        // Not called by Circuit
        override fun onAbandoned() = Unit

        // Nothing to do here
        override fun onRemembered() = Unit
      }
    }
    .scope
}
