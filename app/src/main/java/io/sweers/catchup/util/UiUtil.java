package io.sweers.catchup.util;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.support.annotation.AttrRes;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.UiThread;
import android.util.TypedValue;

public final class UiUtil {

  private static final TypedValue TYPED_VALUE = new TypedValue();

  private UiUtil() {
    throw new InstantiationError("No instances.");
  }

  @ColorInt
  @UiThread
  public static int resolveAttribute(@NonNull Context context, @AttrRes int resId) {
    Resources.Theme theme = context.getTheme();
    theme.resolveAttribute(resId, TYPED_VALUE, true);
    @ColorInt int color = TYPED_VALUE.data;
    return color;
  }

  public static boolean isInNightMode(@NonNull Context context) {
    Configuration conf = context.getResources().getConfiguration();
    return (conf.uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
  }

}
