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
package io.sweers.catchup.util

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import org.junit.Test

@ExperimentalStdlibApi
class MoshiExtTest {

  private val moshi = Moshi.Builder().build()

  @Test
  fun basicList() {
    val expected = moshi.adapter<List<String>>(
        Types.newParameterizedType(List::class.java, String::class.java)).nonNull()
    val actual = moshi.adapter<List<String>>()

    assertThat(actual.toString()).isEqualTo(expected.toString())
  }

  @Test
  fun basicNullableList() {
    val expected = moshi.adapter<List<String>>(
        Types.newParameterizedType(List::class.java, String::class.java)).nullSafe()
    val actual = moshi.adapter<List<String>?>()

    assertThat(actual.toString()).isEqualTo(expected.toString())
  }

  @Test
  fun basicTaco() {
    val expected = moshi.adapter<Taco<String>>(
        Types.newParameterizedType(Taco::class.java, String::class.java)).nonNull()
    val actual = moshi.adapter<Taco<String>>()

    assertThat(actual.toString()).isEqualTo(expected.toString())
  }

  @Test
  fun basicNullableTaco() {
    val expected = moshi.adapter<Taco<String>>(
        Types.newParameterizedType(Taco::class.java, String::class.java)).nullSafe()
    val actual = moshi.adapter<Taco<String>?>()

    assertThat(actual.toString()).isEqualTo(expected.toString())
  }

  @Test
  fun basicNonGeneric() {
    val expected = moshi.adapter(String::class.java).nonNull()
    val actual = moshi.adapter<String>()

    assertThat(actual.toString()).isEqualTo(expected.toString())
  }

  @Test
  fun basicNullableNonGeneric() {
    val expected = moshi.adapter(String::class.java).nullSafe()
    val actual = moshi.adapter<String?>()

    assertThat(actual.toString()).isEqualTo(expected.toString())
  }

  @Test
  fun basicInnerBuiltinNullable() {
    // We can't actually access inner nullability via typeOf(). This test passes because by default Moshi's collection built-ins have nullSafe elements by default
    // What we might actually want is to be able to proactively call nonNull() on non-nullable inner types. Maybe have JsonAdapter expose a "hasNullHandling()" method?
    val expected = moshi.adapter<List<Int?>>(Types.newParameterizedType(List::class.java, Int::class.javaObjectType)).nonNull()
    val actual = moshi.adapter<List<Int?>>()

    assertThat(actual.toString()).isEqualTo(expected.toString())
  }
}

class Taco<T>
