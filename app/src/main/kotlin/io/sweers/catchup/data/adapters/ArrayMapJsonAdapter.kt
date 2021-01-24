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

import androidx.collection.ArrayMap
import com.google.common.collect.ImmutableMap
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

interface MapUpdater<K, V> {
  fun put(key: K, value: V): V?
  fun getMap(): Map<K, V>

  fun interface Factory {
    fun create(): MapUpdater<*, *>
  }
}

// Standard LinkedHashMap
class LinkedHashMapMapUpdater<K, V>(
  initialCapacity: Int = 16,
  loadFactor: Float = 0.75f
) : MapUpdater<K, V> {

  private val map = LinkedHashMap<K, V>(initialCapacity, loadFactor)

  override fun put(key: K, value: V) = map.put(key, value)

  override fun getMap() = map
}

// Android ArrayMap
class ArrayMapUpdater<K, V>(initialCapacity: Int = 0) : MapUpdater<K, V> {
  private val map = ArrayMap<K, V>(initialCapacity)

  override fun put(key: K, value: V) = map.put(key, value)

  override fun getMap() = map
}

// Guava ImmutableMap
class ImmutableMapUpdater<K, V>(expectedSize: Int = 4) : MapUpdater<K, V> {
  private val builder = ImmutableMap.builderWithExpectedSize<K, V>(expectedSize)

  override fun put(key: K, value: V): V? {
    builder.put(key, value)
    // Guava doesn't permit duplicate keys and will throw
    return null
  }

  override fun getMap() = builder.build()
}

/**
 * Converts maps with string keys to JSON objects.
 */
class CustomMapJsonAdapter<K, V> private constructor(
  moshi: Moshi,
  keyType: Type,
  valueType: Type,
  private val mapUpdaterFactory: MapUpdater.Factory,
  private val toJsonDelegate: JsonAdapter<Map<K, V>>
) : JsonAdapter<Map<K, V>>() {

  private val keyAdapter = moshi.adapter<K>(keyType)
  private val valueAdapter = moshi.adapter<V>(valueType)

  override fun toJson(writer: JsonWriter, map: Map<K, V>?) = toJsonDelegate.toJson(writer, map)

  override fun fromJson(reader: JsonReader): Map<K, V> {
    @Suppress("UNCHECKED_CAST")
    val mapUpdater = mapUpdaterFactory.create() as MapUpdater<K, V>
    reader.beginObject()
    while (reader.hasNext()) {
      reader.promoteNameToValue()
      val name = keyAdapter.fromJson(reader) ?: throw JsonDataException(
        "Unexpected null key at ${reader.path}"
      )
      val value = valueAdapter.fromJson(reader) ?: throw JsonDataException(
        "Unexpected null value at ${reader.path}"
      )
      val replaced = mapUpdater.put(name, value)
      if (replaced != null) {
        throw JsonDataException(
          "Map key '$name' has multiple values at path ${reader.path}"
        )
      }
    }
    reader.endObject()
    return mapUpdater.getMap()
  }

  override fun toString(): String {
    return "CustomMapJsonAdapter($keyAdapter=$valueAdapter)"
  }

  companion object {
    /**
     * Returns a new [JsonAdapter.Factory] for handling _all_ [Map] types with the given
     * [mapUpdaterFactory]]
     */
    @JvmStatic
    fun newFactory(mapUpdaterFactory: MapUpdater.Factory): Factory = object : Factory {
      override fun create(type: Type, annotations: Set<Annotation>, moshi: Moshi): JsonAdapter<*>? {
        if (annotations.isEmpty() && type is ParameterizedType) {
          val rawType = Types.getRawType(type)
          if (rawType == Map::class.java) {
            val keyAndValue = type.actualTypeArguments
            val toJsonDelegate = moshi.nextAdapter<Map<Any, Any>>(this, type, annotations)
            return CustomMapJsonAdapter(
              moshi,
              keyAndValue[0],
              keyAndValue[1],
              mapUpdaterFactory,
              toJsonDelegate
            ).nullSafe()
          }
        }
        return null
      }
    }
  }
}
