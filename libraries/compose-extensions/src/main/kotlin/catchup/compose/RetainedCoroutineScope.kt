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
          scope.cancel()
        }

        // Not called by Circuit
        override fun onAbandoned() = Unit

        // Nothing to do here
        override fun onRemembered() = Unit
      }
    }
    .scope
}
