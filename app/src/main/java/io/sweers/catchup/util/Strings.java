package io.sweers.catchup.util;

public final class Strings {
  private Strings() {
    throw new InstantiationError();
  }

  public static boolean isBlank(CharSequence string) {
    return (string == null || string.toString().trim().length() == 0);
  }

  public static String valueOrDefault(String string, String defaultString) {
    return isBlank(string) ? defaultString : string;
  }

  public static String truncateAt(String string, int length) {
    return string.length() > length ? string.substring(0, length) : string;
  }

  public static String capitalize(String input) {
    if (isBlank(input)) {
      return input;
    } else if (input.length() == 1) {
      return input.toUpperCase();
    }
    return Character.toUpperCase(input.charAt(0)) + input.substring(1);
  }
}
