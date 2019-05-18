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
package io.sweers.catchup.data.adapters

import androidx.collection.ArraySet
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonAdapter.Factory
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.lang.reflect.Type

/** Converts collection types to JSON arrays containing their converted contents.  */
abstract class ArrayCollectionJsonAdapter<C : MutableCollection<T>, T> private constructor(
  private val elementAdapter: JsonAdapter<T>
) : JsonAdapter<C>() {

  internal abstract fun newCollection(): C

  override fun fromJson(reader: JsonReader): C? {
    val result = newCollection()
    reader.beginArray()
    while (reader.hasNext()) {
      result.add(elementAdapter.fromJson(reader)!!)
    }
    reader.endArray()
    return result
  }

  override fun toJson(writer: JsonWriter, value: C?) {
    writer.beginArray()
    for (element in value!!) {
      elementAdapter.toJson(writer, element)
    }
    writer.endArray()
  }

  override fun toString(): String {
    return elementAdapter.toString() + ".collection()"
  }

  companion object {
    @JvmField
    val FACTORY = Factory { type, annotations, moshi ->
      if (annotations.isEmpty()) {
        val rawType = Types.getRawType(type)
        if (rawType.isAssignableFrom(Set::class.java)) {
          newSetAdapter<Any>(type, moshi).nullSafe()
        } else if (rawType.isAssignableFrom(Collection::class.java)) {
          newListAdapter<Any>(type, moshi).nullSafe()
        }
      }
      null
    }

    private fun <T> newListAdapter(type: Type, moshi: Moshi): JsonAdapter<MutableCollection<T>> {
      val elementType = Types.collectionElementType(type, MutableCollection::class.java)
      val elementAdapter = moshi.adapter<T>(elementType)
      return object : ArrayCollectionJsonAdapter<MutableCollection<T>, T>(elementAdapter) {
        override fun newCollection(): MutableCollection<T> {
          return ArraySet()
        }
      }
    }

    private fun <T> newSetAdapter(type: Type, moshi: Moshi): JsonAdapter<MutableCollection<T>> {
      val elementType = Types.collectionElementType(type, Collection::class.java)
      val elementAdapter = moshi.adapter<T>(elementType)
      return object : ArrayCollectionJsonAdapter<MutableCollection<T>, T>(elementAdapter) {
        override fun newCollection(): MutableCollection<T> {
          return ArraySet()
        }
      }
    }
  }
}
