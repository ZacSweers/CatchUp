/*
 * Copyright (c) 2017 Zac Sweers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.sweers.catchup.util;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.support.annotation.AttrRes;
import android.support.annotation.ColorInt;
import android.support.annotation.FloatRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.v7.graphics.Palette;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;

public final class UiUtil {

  private static final TypedValue TYPED_VALUE = new TypedValue();

  private UiUtil() {
    throw new InstantiationError();
  }

  @ColorInt @UiThread
  public static int resolveAttribute(@NonNull Context context, @AttrRes int resId) {
    Resources.Theme theme = context.getTheme();
    theme.resolveAttribute(resId, TYPED_VALUE, true);
    @ColorInt int color = TYPED_VALUE.data;
    return color;
  }

  public static boolean isInNightMode(@NonNull Context context) {
    Configuration conf = context.getResources()
        .getConfiguration();
    return (conf.uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
  }

  /**
   * Creates a selector drawable that is API-aware. This will create a ripple for Lollipop+ and
   * supports masks. If this is pre-lollipop and no mask is provided, it will fall back to a simple
   * {@link StateListDrawable} with the color as its pressed and focused states.
   *
   * @param color Selector color
   * @param mask Mask drawable for ripples to be bound to
   * @return The drawable if successful, or null if not valid for this case (masked on pre-lollipop)
   */
  @Nullable public static Drawable createColorSelector(@ColorInt int color,
      @Nullable Drawable mask) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      return new RippleDrawable(ColorStateList.valueOf(color), null, mask);
    } else if (mask == null) {
      ColorDrawable colorDrawable = new ColorDrawable(color);
      StateListDrawable statefulDrawable = new StateListDrawable();
      statefulDrawable.setEnterFadeDuration(200);
      statefulDrawable.setExitFadeDuration(200);
      statefulDrawable.addState(new int[] { android.R.attr.state_pressed }, colorDrawable);
      statefulDrawable.addState(new int[] { android.R.attr.state_focused }, colorDrawable);
      statefulDrawable.addState(new int[] {}, null);
      return statefulDrawable;
    } else {
      // We don't do it on pre-lollipop because normally selectors can't abide by a mask
      return null;
    }
  }

  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  public static RippleDrawable createRipple(@ColorInt int color, boolean bounded) {
    return new RippleDrawable(ColorStateList.valueOf(color),
        null,
        bounded ? new ColorDrawable(Color.WHITE) : null);
  }

  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  public static RippleDrawable createRipple(@ColorInt int color,
      @FloatRange(from = 0f,
                  to = 1f) float alpha,
      boolean bounded) {
    color = ColorUtils.modifyAlpha(color, alpha);
    return new RippleDrawable(ColorStateList.valueOf(color),
        null,
        bounded ? new ColorDrawable(Color.WHITE) : null);
  }

  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  public static RippleDrawable createRipple(@NonNull Palette palette,
      @FloatRange(from = 0f,
                  to = 1f) float darkAlpha,
      @FloatRange(from = 0f,
                  to = 1f) float lightAlpha,
      @ColorInt int fallbackColor,
      boolean bounded) {
    int rippleColor = fallbackColor;
    if (palette != null) {
      // try the named swatches in preference order
      if (palette.getVibrantSwatch() != null) {
        rippleColor = ColorUtils.modifyAlpha(palette.getVibrantSwatch()
            .getRgb(), darkAlpha);
      } else if (palette.getLightVibrantSwatch() != null) {
        rippleColor = ColorUtils.modifyAlpha(palette.getLightVibrantSwatch()
            .getRgb(), lightAlpha);
      } else if (palette.getDarkVibrantSwatch() != null) {
        rippleColor = ColorUtils.modifyAlpha(palette.getDarkVibrantSwatch()
            .getRgb(), darkAlpha);
      } else if (palette.getMutedSwatch() != null) {
        rippleColor = ColorUtils.modifyAlpha(palette.getMutedSwatch()
            .getRgb(), darkAlpha);
      } else if (palette.getLightMutedSwatch() != null) {
        rippleColor = ColorUtils.modifyAlpha(palette.getLightMutedSwatch()
            .getRgb(), lightAlpha);
      } else if (palette.getDarkMutedSwatch() != null) {
        rippleColor = ColorUtils.modifyAlpha(palette.getDarkMutedSwatch()
            .getRgb(), darkAlpha);
      }
    }
    return new RippleDrawable(ColorStateList.valueOf(rippleColor),
        null,
        bounded ? new ColorDrawable(Color.WHITE) : null);
  }

  /**
   * Determine if the navigation bar will be on the bottom of the screen, based on logic in
   * PhoneWindowManager.
   */
  public static boolean isNavBarOnBottom(@NonNull Context context) {
    final Resources res = context.getResources();
    final Configuration cfg = context.getResources()
        .getConfiguration();
    final DisplayMetrics dm = res.getDisplayMetrics();
    boolean canMove = (dm.widthPixels != dm.heightPixels && cfg.smallestScreenWidthDp < 600);
    return (!canMove || dm.widthPixels < dm.heightPixels);
  }

  public static void setLightStatusBar(@NonNull View view) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      int flags = view.getSystemUiVisibility();
      // TODO noop if it's already set
      flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
      view.setSystemUiVisibility(flags);
    }
  }

  public static void clearLightStatusBar(@NonNull View view) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      int flags = view.getSystemUiVisibility();
      // TODO noop if it's already not set
      flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
      view.setSystemUiVisibility(flags);
    }
  }
}
