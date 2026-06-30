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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import catchup.base.ui.SystemBarColorController
import catchup.base.ui.rememberSystemBarColorController

class ConditionalSystemUiColors(
  private val systemBarColorController: SystemBarColorController,
  initialStatusBarDarkContent: Boolean,
  initialNavBarDarkContent: Boolean,
) {
  private var storedStatusBarDarkContent by mutableStateOf(initialStatusBarDarkContent)
  private var storedNavBarDarkContent by mutableStateOf(initialNavBarDarkContent)

  fun save() {
    storedStatusBarDarkContent = systemBarColorController.statusBarDarkContentEnabled
    storedNavBarDarkContent = systemBarColorController.navigationBarDarkContentEnabled
  }

  fun restore() {
    systemBarColorController.statusBarDarkContentEnabled = storedStatusBarDarkContent
    systemBarColorController.navigationBarDarkContentEnabled = storedNavBarDarkContent
  }
}

// TODO if dark mode changes during this, it will restore the wrong colors. What do we do?
@Composable
fun rememberConditionalSystemUiColors(
  systemBarColorController: SystemBarColorController = rememberSystemBarColorController()
): ConditionalSystemUiColors {
  return ConditionalSystemUiColors(
    systemBarColorController,
    systemBarColorController.statusBarDarkContentEnabled,
    systemBarColorController.navigationBarDarkContentEnabled,
  )
}
