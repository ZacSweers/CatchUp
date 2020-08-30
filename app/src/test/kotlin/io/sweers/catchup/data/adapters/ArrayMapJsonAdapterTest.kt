/*
 * Copyright (C) 2020. Zac Sweers
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
package io.sweers.catchup.data.adapters

import androidx.collection.ArrayMap
import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import org.junit.Test

class ArrayMapJsonAdapterTest {
  @Test
  fun testMap() {
    val moshi = Moshi.Builder()
      .add(ArrayMapJsonAdapter.FACTORY)
      .build()
    val map = mapOf("1" to "2", "3" to "4")
    val adapter = moshi.adapter<Map<String, String>>(Types.newParameterizedType(Map::class.java, String::class.java, String::class.java))
    val jsonString = adapter.toJson(map)
    assertThat(jsonString).isEqualTo("{\"1\":\"2\",\"3\":\"4\"}")
    val parsedMap = adapter.fromJson(jsonString)
    assertThat(parsedMap).isEqualTo(map)
    assertThat(parsedMap).isInstanceOf(ArrayMap::class.java)
  }
}
