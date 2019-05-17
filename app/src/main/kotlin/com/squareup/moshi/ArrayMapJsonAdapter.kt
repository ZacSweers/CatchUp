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
package com.squareup.moshi

import androidx.collection.ArrayMap
import com.squareup.moshi.JsonAdapter.Factory
import java.lang.reflect.Type

/**
 * Converts maps with string keys to JSON objects.
 */
class ArrayMapJsonAdapter<K, V>(
  moshi: Moshi,
  keyType: Type,
  valueType: Type
) : JsonAdapter<Map<K, V>>() {

  private val keyAdapter: JsonAdapter<K> = moshi.adapter<K>(keyType)
  private val valueAdapter: JsonAdapter<V> = moshi.adapter<V>(valueType)

  override fun toJson(writer: JsonWriter, map: Map<K, V>?) {
    writer.beginObject()
    for ((key, value) in map!!) {
      if (key == null) {
        throw JsonDataException("Map key is null at path " + writer.path)
      }
      writer.promoteValueToName()
      keyAdapter.toJson(writer, key)
      valueAdapter.toJson(writer, value)
    }
    writer.endObject()
  }

  override fun fromJson(reader: JsonReader): Map<K, V>? {
    val result = ArrayMap<K, V>()
    reader.beginObject()
    while (reader.hasNext()) {
      reader.promoteNameToValue()
      val name = keyAdapter.fromJson(reader)
      val value = valueAdapter.fromJson(reader)
      val replaced = result.put(name, value)
      if (replaced != null) {
        throw JsonDataException("Map key '" + name + "' has multiple values at path " +
            reader.path)
      }
    }
    reader.endObject()
    return result
  }

  override fun toString(): String {
    return "JsonAdapter($keyAdapter=$valueAdapter)"
  }

  companion object {
    @JvmField
    val FACTORY = Factory { type, annotations, moshi ->
      if (annotations.isEmpty()) {
        val rawType = Types.getRawType(type)
        if (rawType == Map::class.java) {
          val keyAndValue = Types.mapKeyAndValueTypes(type, rawType)
          ArrayMapJsonAdapter<Any, Any>(moshi, keyAndValue[0], keyAndValue[1]).nullSafe()
        }
      }
      null
    }
  }
}
