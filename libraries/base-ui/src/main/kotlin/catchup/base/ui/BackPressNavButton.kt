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
package catchup.base.ui

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import catchup.base.ui.NavButtonType.BACK
import catchup.ui.core.R

enum class NavButtonType(val icon: ImageVector, @StringRes val contentDescription: Int) {
  BACK(Icons.AutoMirrored.Filled.ArrowBack, R.string.catchup_baseui_back),
  CLOSE(Icons.Filled.Close, R.string.catchup_baseui_close),
}

@Composable
fun BackPressNavButton(modifier: Modifier = Modifier, type: NavButtonType = BACK) {
  val onBackPressedDispatcher = LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher
  NavButton(modifier, type, onBackPressedDispatcher::onBackPressed)
}

@Composable
fun NavButton(modifier: Modifier = Modifier, type: NavButtonType = BACK, onBackPress: () -> Unit) {
  IconButton(modifier = modifier, onClick = onBackPress) {
    Icon(
      type.icon,
      modifier = Modifier.size(24.dp),
      // Hardcoded in previews because lol compose tooling
      contentDescription =
        if (LocalInspectionMode.current) {
          "Close"
        } else {
          stringResource(type.contentDescription)
        },
    )
  }
}
