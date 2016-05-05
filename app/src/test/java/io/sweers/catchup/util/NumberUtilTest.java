package io.sweers.catchup.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Locale;

import static com.google.common.truth.Truth.assertThat;

public final class NumberUtilTest {
  public static final long[] NUMBERS = {0, 5, 999, 1_000, -5_821, 10_500, -101_800, 2_000_000, -7_800_000, 92_150_000, 123_200_000, 9_999_999, 999_999_999_999_999_999L, 1_230_000_000_000_000L, Long.MIN_VALUE, Long.MAX_VALUE};
  public static final Locale DEFAULT_LOCALE = Locale.US;
  public static final String[] EXPECTED = {"0", "5", "999", "1k", "-5.8k", "10k", "-101k", "2M", "-7.8M", "92M", "123M", "9.9M", "999P", "1.2P", "-9.2E", "9.2E"};

  @Before
  public void setUp() {
    Locale.setDefault(DEFAULT_LOCALE);
  }

  @After
  public void tearDown() {
    Locale.setDefault(DEFAULT_LOCALE);
  }

  @Test
  public void testFormat() {
    for (int i = 0; i < NUMBERS.length; i++) {
      long n = NUMBERS[i];
      String formatted = NumberUtil.format(n);
      assertThat(formatted).isEqualTo(EXPECTED[i]);
    }
  }

  @Test
  public void testFormatIT() {
    Locale.setDefault(Locale.ITALIAN);
    for (int i = 0; i < NUMBERS.length; i++) {
      long n = NUMBERS[i];
      String formatted = NumberUtil.format(n);
      assertThat(formatted).isEqualTo(EXPECTED[i].replace('.', ','));
    }
  }

  @Test
  public void testShorten() {
    for (int i = 0; i < NUMBERS.length; i++) {
      long n = NUMBERS[i];
      String formatted = NumberUtil.shorten(n);
      assertThat(formatted).isEqualTo(EXPECTED[i]);
    }
  }
}
