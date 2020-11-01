package dev.zacsweers.catchup.compose

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.accessibilityLabel
import androidx.compose.ui.semantics.semantics

@Composable
fun Modifier.accessibilityLabel(@StringRes resId: Int) = composed {
  val res = stringResource(resId)
  semantics {
    accessibilityLabel = res
  }
}