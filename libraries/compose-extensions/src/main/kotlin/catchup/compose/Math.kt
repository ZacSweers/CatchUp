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

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Interpolates between two [Offset] values using an arc motion.
 *
 * Given two points `start` and `stop`, the function calculates a half-circle arc that connects
 * them. The returned [Offset] represents a point on that arc based on the provided `fraction`.
 *
 * Illustration:
 * ```
 *    start   .-.
 *           /   \
 *          |  O  | <-- Arc center (midpoint between `start` and `stop`)
 *           \   /
 *    stop    '-'
 * ```
 *
 * @param start The starting [Offset] (represents fraction = 0).
 * @param stop The ending [Offset] (represents fraction = 1).
 * @param fraction The fraction of the motion, ranging from 0f to 1f.
 * @return The [Offset] on the arc corresponding to the provided fraction.
 */
fun arcLerp(start: Offset, stop: Offset, fraction: Float): Offset {
  // Ensure fraction is within [0, 1]
  val clampedFraction = fraction.coerceIn(0f, 1f)

  // Midpoint (center of the circle)
  val centerX = (start.x + stop.x) / 2
  val centerY = (start.y + stop.y) / 2

  // Radius (half the distance between start and stop)
  val radiusX = (stop.x - start.x) / 2
  val radiusY = (stop.y - start.y) / 2

  // Angle based on fraction
  val theta = Math.PI * clampedFraction

  // Calculate point on the arc
  val x = centerX + radiusX * sin(theta).toFloat()
  val y = centerY - radiusY * cos(theta).toFloat()

  return Offset(x, y)
}

/** @see arcLerp */
@Suppress("unused")
fun arcLerp(start: IntOffset, stop: IntOffset, fraction: Float): IntOffset {
  // Ensure fraction is within [0, 1]
  val clampedFraction = fraction.coerceIn(0f, 1f)

  // Midpoint (center of the circle)
  val centerX = (start.x + stop.x) / 2
  val centerY = (start.y + stop.y) / 2

  // Radius (half the distance between start and stop)
  val radiusX = (stop.x - start.x) / 2
  val radiusY = (stop.y - start.y) / 2

  // Angle based on fraction
  val theta = Math.PI * clampedFraction

  // Calculate point on the arc
  val x = (centerX + radiusX * sin(theta)).roundToInt()
  val y = (centerY - radiusY * cos(theta)).roundToInt()

  return IntOffset(x, y)
}
