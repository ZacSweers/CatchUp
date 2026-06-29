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

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

val LocalDynamicTheme = compositionLocalOf { false }

@Suppress("NOTHING_TO_INLINE") // Required in K2
@Composable
inline fun dynamicAwareColor(
  regularColor: @Composable () -> Color,
  dynamicColor: @Composable () -> Color,
  // Trigger recomposition if the context changes
  @Suppress("UNUSED_PARAMETER") context: Context = LocalContext.current,
): Color {
  val isDynamic = LocalDynamicTheme.current
  return if (isDynamic) {
    dynamicColor()
  } else {
    regularColor()
  }
}
