package catchup.base.ui

import androidx.compose.runtime.Composable
import catchup.di.AppScope
import catchup.di.SingleIn
import com.slack.circuit.runtime.Navigator
import com.squareup.anvil.annotations.ContributesBinding
import javax.inject.Inject

interface RootContent {
  @Composable fun Content(navigator: Navigator, content: @Composable () -> Unit)
}

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class DefaultRootContent @Inject constructor() : RootContent {
  @Composable
  override fun Content(navigator: Navigator, content: @Composable () -> Unit) {
    content()
  }
}
