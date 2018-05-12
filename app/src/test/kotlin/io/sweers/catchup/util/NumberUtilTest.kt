/*
 * Copyright (c) 2018 Zac Sweers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.sweers.catchup.util

import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.Locale

class NumberUtilTest {

  companion object {
    val NUMBERS = longArrayOf(0, 5, 999, 1000, -5821, 10500, -101800, 2000000, -7800000, 92150000,
        123200000, 9999999, 999_999_999_999_999_999L, 1_230_000_000_000_000L,
        java.lang.Long.MIN_VALUE, java.lang.Long.MAX_VALUE)
    val DEFAULT_LOCALE: Locale = Locale.US
    val EXPECTED = arrayOf("0", "5", "999", "1k", "-5.8k", "10k", "-101k", "2M", "-7.8M", "92M",
        "123M", "9.9M", "999P", "1.2P", "-9.2E", "9.2E")
  }

  @Before
  fun setUp() {
    Locale.setDefault(DEFAULT_LOCALE)
  }

  @After
  fun tearDown() {
    Locale.setDefault(DEFAULT_LOCALE)
  }

  @Test
  fun testFormat() {
    for (i in NUMBERS.indices) {
      val n = NUMBERS[i]
      val formatted = n.format()
      assertThat(formatted).isEqualTo(EXPECTED[i])
    }
  }

  @Test
  fun testFormatIT() {
    Locale.setDefault(Locale.ITALIAN)
    for (i in NUMBERS.indices) {
      val n = NUMBERS[i]
      val formatted = n.format()
      assertThat(formatted).isEqualTo(EXPECTED[i].replace('.', ','))
    }
  }

  @Test
  fun testShorten() {
    for (i in NUMBERS.indices) {
      val n = NUMBERS[i]
      val formatted = n.shorten()
      assertThat(formatted).isEqualTo(EXPECTED[i])
    }
  }
}
