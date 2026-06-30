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

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer

@Composable
fun <T> Wigglable(
  target: T,
  modifier: Modifier = Modifier,
  shouldWiggle: (old: T, new: T) -> Boolean = { old, new -> old != new },
  content: @Composable BoxScope.() -> Unit,
) {
  var triggerWiggle by remember { mutableStateOf(false) }

  val previous = previous(target, target)
  LaunchedEffect(target) { triggerWiggle = shouldWiggle(previous, target) }

  val angle by
    animateFloatAsState(
      targetValue = if (triggerWiggle) 10f else 0f,
      animationSpec = tween(durationMillis = 200),
      label = "Wiggle",
    )

  val scale by
    animateFloatAsState(
      targetValue = if (triggerWiggle) 1.2f else 1f,
      animationSpec = tween(durationMillis = 200),
      label = "Wiggle",
    )

  LaunchedEffect(angle) {
    if (angle == 10f) {
      triggerWiggle = false
    }
  }

  Box(modifier = modifier.graphicsLayer(rotationZ = angle).scale(scale), content = content)
}
