package io.sweers.catchup.base.ui

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import catchup.ui.core.R

enum class NavButtonType(val icon: ImageVector, @StringRes val contentDescription: Int) {
  BACK(Icons.Filled.ArrowBack, R.string.back),
  CLOSE(Icons.Filled.Close, R.string.close)
}

@Composable
fun BackPressNavButton(modifier: Modifier = Modifier, type: NavButtonType = NavButtonType.BACK) {
  val onBackPressedDispatcher = LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher
  NavButton(modifier, type, onBackPressedDispatcher::onBackPressed)
}

@Composable
fun NavButton(
  modifier: Modifier = Modifier,
  type: NavButtonType = NavButtonType.BACK,
  onBackPress: () -> Unit
) {
  IconButton(
    modifier = modifier,
    onClick = onBackPress,
  ) {
    Icon(
      type.icon,
      modifier = Modifier.size(24.dp),
      contentDescription = stringResource(type.contentDescription)
    )
  }
}
