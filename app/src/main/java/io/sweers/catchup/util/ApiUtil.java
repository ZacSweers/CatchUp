package io.sweers.catchup.util;

import android.os.Build;

public final class ApiUtil {

  private ApiUtil() {
    throw new InstantiationError();
  }

  public static boolean isM() {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
  }

  public static boolean isL() {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
  }

  public static boolean isKK() {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
  }

}
