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
import androidx.compose.runtime.retain.RetainObserver
import androidx.compose.runtime.retain.retain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel

/** https://chrisbanes.me/posts/retaining-beyond-viewmodels/ */
@Composable
fun rememberRetainedCoroutineScope(): CoroutineScope {
  return retain("coroutine_scope") {
      object : RetainObserver {
        val scope = CoroutineScope(Dispatchers.Main + Job())

        override fun onRetired() {
          scope.cancel(null)
        }

        override fun onUnused() {
          scope.cancel(null)
        }

        override fun onRetained() = Unit

        override fun onEnteredComposition() = Unit

        override fun onExitedComposition() = Unit
      }
    }
    .scope
}
