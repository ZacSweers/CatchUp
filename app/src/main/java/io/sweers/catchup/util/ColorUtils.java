/*
 * Copyright 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.sweers.catchup.util;

import android.graphics.Bitmap;
import android.support.annotation.CheckResult;
import android.support.annotation.ColorInt;
import android.support.annotation.FloatRange;
import android.support.annotation.IntDef;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.graphics.Palette;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Utility methods for working with colors.
 */
public class ColorUtils {

  public static final int IS_LIGHT = 0;
  public static final int IS_DARK = 1;
  public static final int LIGHTNESS_UNKNOWN = 2;

  private ColorUtils() {
    throw new InstantiationError();
  }

  /**
   * Set the alpha component of {@code color} to be {@code alpha}.
   */
  @CheckResult
  @ColorInt
  public static int modifyAlpha(
      @ColorInt int color,
      @IntRange(from = 0, to = 255) int alpha) {
    return (color & 0x00ffffff) | (alpha << 24);
  }

  /**
   * Set the alpha component of {@code color} to be {@code alpha}.
   */
  @CheckResult
  @ColorInt
  public static int modifyAlpha(
      @ColorInt int color,
      @FloatRange(from = 0f, to = 1f) float alpha) {
    return modifyAlpha(color, (int) (255f * alpha));
  }

  /**
   * Checks if the most populous color in the given palette is dark
   * <p/>
   * Annoyingly we have to return this Lightness 'enum' rather than a boolean as palette isn't
   * guaranteed to find the most populous color.
   */
  @Lightness
  public static int isDark(Palette palette) {
    Palette.Swatch mostPopulous = getMostPopulousSwatch(palette);
    if (mostPopulous == null) return LIGHTNESS_UNKNOWN;
    return isDark(mostPopulous.getHsl()) ? IS_DARK : IS_LIGHT;
  }

  @Nullable
  public static Palette.Swatch getMostPopulousSwatch(Palette palette) {
    Palette.Swatch mostPopulous = null;
    if (palette != null) {
      for (Palette.Swatch swatch : palette.getSwatches()) {
        if (mostPopulous == null || swatch.getPopulation() > mostPopulous.getPopulation()) {
          mostPopulous = swatch;
        }
      }
    }
    return mostPopulous;
  }

  /**
   * Determines if a given bitmap is dark. This extracts a palette inline so should not be called
   * with a large image!!
   * <p/>
   * Note: If palette fails then check the color of the central pixel
   */
  public static boolean isDark(
      @NonNull Bitmap bitmap) {
    return isDark(bitmap, bitmap.getWidth() / 2, bitmap.getHeight() / 2);
  }

  /**
   * Determines if a given bitmap is dark. This extracts a palette inline so should not be called
   * with a large image!! If palette fails then check the color of the specified pixel
   */
  public static boolean isDark(
      @NonNull Bitmap bitmap,
      int backupPixelX,
      int backupPixelY) {
    // first try palette with a small color quant size
    Palette palette = Palette.from(bitmap).maximumColorCount(3).generate();
    if (palette.getSwatches().size() > 0) {
      return isDark(palette) == IS_DARK;
    } else {
      // if palette failed, then check the color of the specified pixel
      return isDark(bitmap.getPixel(backupPixelX, backupPixelY));
    }
  }

  /**
   * Check that the lightness value (0–1)
   */
  public static boolean isDark(float[] hsl) { // @Size(3)
    return hsl[2] < 0.5f;
  }

  /**
   * Convert to HSL & check that the lightness value
   */
  public static boolean isDark(
      @ColorInt int color) {
    float[] hsl = new float[3];
    android.support.v4.graphics.ColorUtils.colorToHSL(color, hsl);
    return isDark(hsl);
  }

  @Retention(RetentionPolicy.SOURCE)
  @IntDef({IS_LIGHT, IS_DARK, LIGHTNESS_UNKNOWN})
  public @interface Lightness {
  }

}
