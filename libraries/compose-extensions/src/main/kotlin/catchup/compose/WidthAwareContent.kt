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

import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize

@Composable
fun columnCount(minColumns: Int, minColumnWidth: Dp = 250.dp): Int {
  val availableWidth = rememberAvailableWidth()
  val minWidthPx = with(LocalDensity.current) { minColumnWidth.toPx() }
  return (availableWidth / minWidthPx).toInt().coerceAtLeast(minColumns)
}

@Suppress("ComposeContentEmitterReturningValues")
@Composable
fun rememberAvailableWidth(): Float {
  var availableWidth by remember { mutableFloatStateOf(0f) }
  HorizontalDivider(
    modifier =
      Modifier.onGloballyPositioned { layoutCoordinates ->
        val size = layoutCoordinates.size.toSize()
        availableWidth = size.width
      }
  )
  return availableWidth
}
