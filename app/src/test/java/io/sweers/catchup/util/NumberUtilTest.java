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
