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

import androidx.collection.ArraySet
import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import io.sweers.catchup.data.adapters.CollectionUpdater.Factory
import org.junit.Test

class ArrayCollectionsJsonAdapterTest {
  @Test
  fun testList() {
    val moshi = Moshi.Builder()
      .add(
        CustomCollectionJsonAdapter.newFactory(
          listUpdaterFactory = object : Factory {
            override fun <E, C : Collection<E>> create(): CollectionUpdater<E, C> {
              @Suppress("UNCHECKED_CAST")
              return ArrayListCollectionUpdater<E>() as CollectionUpdater<E, C>
            }
          }
        )
      )
      .build()
    val collection = listOf("one", "two", "three")
    val adapter = moshi.adapter<List<String>>(Types.newParameterizedType(List::class.java, String::class.java))
    val jsonString = adapter.toJson(collection)
    assertThat(jsonString).isEqualTo("[\"one\",\"two\",\"three\"]")
    val parsedMap = adapter.fromJson(jsonString)
    assertThat(parsedMap).isEqualTo(collection)
    assertThat(parsedMap).isInstanceOf(ArrayList::class.java)
  }

  @Test
  fun testSet() {
    val moshi = Moshi.Builder()
      .add(
        CustomCollectionJsonAdapter.newFactory(
          setUpdaterFactory = object : Factory {
            override fun <E, C : Collection<E>> create(): CollectionUpdater<E, C> {
              @Suppress("UNCHECKED_CAST")
              return ArraySetCollectionUpdater<E>() as CollectionUpdater<E, C>
            }
          }
        )
      )
      .build()
    val collection = setOf("one", "two", "three")
    val adapter = moshi.adapter<Set<String>>(Types.newParameterizedType(Set::class.java, String::class.java))
    val jsonString = adapter.toJson(collection)
    assertThat(jsonString).isEqualTo("[\"one\",\"two\",\"three\"]")
    val parsedMap = adapter.fromJson(jsonString)
    assertThat(parsedMap).isEqualTo(collection)
    assertThat(parsedMap).isInstanceOf(ArraySet::class.java)
  }
}
