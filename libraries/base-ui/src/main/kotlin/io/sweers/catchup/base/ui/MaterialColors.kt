/*
 * Copyright (C) 2019. Zac Sweers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
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
package io.sweers.catchup.base.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.StateSet
import android.view.View
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.FloatRange
import androidx.core.graphics.ColorUtils
import java.util.ArrayList
import java.util.HashSet

/**
 * A utility class for common color variants used in Material themes.
 */
internal object MaterialColors {

  const val ALPHA_FULL = 1.00f
  const val ALPHA_MEDIUM = 0.54f
  const val ALPHA_DISABLED = 0.38f
  const val ALPHA_LOW = 0.32f
  const val ALPHA_DISABLED_LOW = 0.12f

  /**
   * Returns the color int for the provided theme color attribute, using the [Context] of the
   * provided `view`.
   *
   * @throws IllegalArgumentException if the attribute is not set in the current theme.
   */
  @ColorInt
  fun getColor(view: View, @AttrRes colorAttributeResId: Int): Int {
    return MaterialAttributes.resolveOrThrow(view, colorAttributeResId)
  }

  /**
   * Returns the color int for the provided theme color attribute.
   *
   * @throws IllegalArgumentException if the attribute is not set in the current theme.
   */
  @ColorInt
  fun getColor(
    context: Context,
    @AttrRes colorAttributeResId: Int,
    errorMessageComponent: String
  ): Int {
    return MaterialAttributes.resolveOrThrow(context, colorAttributeResId, errorMessageComponent)
  }

  /**
   * Returns the color int for the provided theme color attribute, or the default value if the
   * attribute is not set in the current theme, using the `view`'s [Context].
   */
  @ColorInt
  fun getColor(
    view: View,
    @AttrRes colorAttributeResId: Int,
    @ColorInt defaultValue: Int
  ): Int {
    return getColor(view.context, colorAttributeResId, defaultValue)
  }

  /**
   * Returns the color int for the provided theme color attribute, or the default value if the
   * attribute is not set in the current theme.
   */
  @ColorInt
  fun getColor(
    context: Context,
    @AttrRes colorAttributeResId: Int,
    @ColorInt defaultValue: Int
  ): Int {
    val typedValue = MaterialAttributes.resolve(context, colorAttributeResId)
    return typedValue?.data ?: defaultValue
  }

  /**
   * Convenience method that wraps [MaterialColors.layer] for layering colors
   * from theme attributes.
   */
  @ColorInt
  @JvmOverloads
  fun layer(
    view: View,
    @AttrRes backgroundColorAttributeResId: Int,
    @AttrRes overlayColorAttributeResId: Int,
    @FloatRange(from = 0.0, to = 1.0) overlayAlpha: Float = 1f
  ): Int {
    val backgroundColor = getColor(view, backgroundColorAttributeResId)
    val overlayColor = getColor(view, overlayColorAttributeResId)
    return layer(backgroundColor, overlayColor, overlayAlpha)
  }

  /**
   * Calculates a color that represents the layering of the `overlayColor` (with `overlayAlpha` applied) on top of the `backgroundColor`.
   */
  @ColorInt
  fun layer(
    @ColorInt backgroundColor: Int,
    @ColorInt overlayColor: Int,
    @FloatRange(from = 0.0, to = 1.0) overlayAlpha: Float
  ): Int {
    val computedAlpha = Math.round(Color.alpha(overlayColor) * overlayAlpha)
    val computedOverlayColor = ColorUtils.setAlphaComponent(overlayColor, computedAlpha)
    return layer(backgroundColor, computedOverlayColor)
  }

  /**
   * Calculates a color that represents the layering of the `overlayColor` on top of the
   * `backgroundColor`.
   */
  @ColorInt
  fun layer(@ColorInt backgroundColor: Int, @ColorInt overlayColor: Int): Int {
    return ColorUtils.compositeColors(overlayColor, backgroundColor)
  }

  /**
   * Calculates a color state list that represents the layering of the `overlayColor` on top
   * of the `backgroundColor` for the given set of `states`. CAUTION: More specific
   * states that have the same color value as a more generic state may be dropped, see example
   * below:
   *
   * states:
   * ```
   * {selected, enabled},
   * {checked, enabled},
   * {enabled},
   * default
   * ```
   *
   * Overlay CSL:
   * ```
   * ""      TRANSPARENT
   * ```
   *
   * Scenario 1
   *
   * Background CSL:
   * ```
   * checked RED
   * ""      GREEN
   * ```
   *
   *
   * Current result:
   * ```
   * enabled, checked RED
   * enabled          GREEN
   * ```
   *
   *
   * Color for state {enabled, checked, selected} --> returns RED # correct
   *
   *
   * Result if iterating top down through CSL to composite each state color:
   * ```
   * enabled, selected GREEN
   * enabled, checked  RED
   * enabled           GREEN
   * ```
   *
   *
   * Color for state {enabled, checked, selected} --> returns GREEN #incorrect
   *
   *
   * Scenario 2
   * Background CSL:
   * ```
   * selected GREEN
   * checked  RED
   * ""       GREEN
   * ```
   *
   * Current result:
   * ```
   * enabled, checked RED
   * enabled          GREEN
   * ```
   *
   * Color for state {enabled, checked, selected} --> returns RED # incorrect
   *
   *
   * Result if iterating top down through CSL to composite each state color:
   * ```
   * enabled, selected GREEN
   * enabled, checked RED
   * enabled          GREEN
   * ```
   *
   * Color for state {enabled, checked, selected} --> returns GREEN # correct
   */
  fun layer(
    backgroundColor: ColorStateList,
    @ColorInt defaultBackgroundColor: Int,
    overlayColor: ColorStateList,
    @ColorInt defaultOverlayColor: Int,
    states: Array<IntArray>
  ): ColorStateList {
    val uniqueColors = ArrayList<Int>()
    val uniqueStateSet = ArrayList<IntArray>()

    // Iterates bottom to top, from least to most specific states.
    for (i in states.indices.reversed()) {
      val curState = states[i]
      val layeredStateColor = layer(
          backgroundColor.getColorForState(curState, defaultBackgroundColor),
          overlayColor.getColorForState(curState, defaultOverlayColor))

      if (shouldAddColorForState(uniqueColors, layeredStateColor, uniqueStateSet, curState)) {
        // Add to the front of the list in original CSL order.
        uniqueColors.add(0, layeredStateColor)
        uniqueStateSet.add(0, curState)
      }
    }

    // Convert lists to arrays.
    val numStates = uniqueColors.size
    val colors = IntArray(numStates)
    val colorStates = arrayOfNulls<IntArray>(numStates)
    for (i in 0 until numStates) {
      colors[i] = uniqueColors[i]
      colorStates[i] = uniqueStateSet[i]
    }
    return ColorStateList(colorStates, colors)
  }

  /**
   * Returns whether the specified @{code color} should be added to a ColorStateList for the
   * specified `state` or if the existing color set and state set already cover it.
   */
  private fun shouldAddColorForState(
    colorSet: List<Int>,
    @ColorInt color: Int,
    stateSet: List<IntArray>,
    state: IntArray?
  ): Boolean {
    HashSet(colorSet)
    if (!colorSet.contains(color)) {
      return true
    }
    for (stateSetItem in stateSet) {
      if (StateSet.stateSetMatches(stateSetItem, state)) {
        return colorSet[stateSet.indexOf(stateSetItem)] != color
      }
    }
    return true
  }
}
