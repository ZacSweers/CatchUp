package io.sweers.catchup.util;

import android.support.annotation.Nullable;

import static java.lang.String.format;

public final class Preconditions {

  private Preconditions() {
    throw new InstantiationError();
  }

  /**
   * Ensures that an object reference passed as a parameter to the calling method is not null.
   *
   * @param reference an object reference
   * @return the non-null reference that was validated
   * @throws NullPointerException if {@code reference} is null
   */
  public static <T> T checkNotNull(T reference) {
    if (reference == null) {
      throw new NullPointerException();
    }
    return reference;
  }

  /**
   * Ensures that an object reference passed as a parameter to the calling method is not null.
   *
   * @param reference    an object reference
   * @param errorMessage the exception message to use if the check fails; will be converted to a
   *                     string using {@link String#valueOf(Object)}
   * @return the non-null reference that was validated
   * @throws NullPointerException if {@code reference} is null
   */
  public static <T> T checkNotNull(T reference, @Nullable Object errorMessage) {
    if (reference == null) {
      throw new NullPointerException(String.valueOf(errorMessage));
    }
    return reference;
  }

  /**
   * Ensures that an object reference passed as a parameter to the calling method is not null.
   *
   * @param reference            an object reference
   * @param errorMessageTemplate a template for the exception message should the check fail. The
   *                             message is formed by replacing each {@code %s} placeholder in the template with an
   *                             argument. These are matched by position - the first {@code %s} gets {@code
   *                             errorMessageArgs[0]}, etc.  Unmatched arguments will be appended to the formatted message
   *                             in square braces. Unmatched placeholders will be left as-is.
   * @param errorMessageArgs     the arguments to be substituted into the message template. Arguments
   *                             are converted to strings using {@link String#valueOf(Object)}.
   * @return the non-null reference that was validated
   * @throws NullPointerException if {@code reference} is null
   */
  public static <T> T checkNotNull(T reference,
                                   @Nullable String errorMessageTemplate,
                                   @Nullable Object... errorMessageArgs) {
    if (reference == null) {
      // If either of these parameters is null, the right thing happens anyway
      if (errorMessageTemplate != null) {
        throw new NullPointerException(format(errorMessageTemplate, errorMessageArgs));
      } else {
        throw new NullPointerException("Input was null.");
      }
    }
    return reference;
  }

}
