package io.sweers.catchup.base.ui

import androidx.compose.runtime.Composable
import com.slack.circuit.runtime.Navigator
import com.squareup.anvil.annotations.ContributesBinding
import dev.zacsweers.catchup.di.AppScope
import dev.zacsweers.catchup.di.SingleIn
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
