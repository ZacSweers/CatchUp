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
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableSet
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.lang.reflect.Type

interface CollectionUpdater<E, out C : Collection<E>> {
  fun add(value: E): Boolean
  fun getCollection(): C

  // Can't be a fun interface because Kotlin forbids type arguments on SAM functions
  interface Factory {
    fun <E, C : Collection<E>> create(): CollectionUpdater<E, C>
  }
}

class ArrayListCollectionUpdater<E>(initialCapacity: Int = 0) : CollectionUpdater<E, List<E>> {
  private val set = ArrayList<E>(initialCapacity)

  override fun add(value: E): Boolean {
    return set.add(value)
  }

  override fun getCollection(): List<E> {
    return set
  }
}

class ImmutableListCollectionUpdater<E>(expectedSize: Int = 4) : CollectionUpdater<E, List<E>> {
  private val set = ImmutableList.builderWithExpectedSize<E>(expectedSize)

  override fun add(value: E): Boolean {
    set.add(value)
    // Guava doesn't indicate add() success
    return false
  }

  override fun getCollection() = set.build()
}

class ArraySetCollectionUpdater<E>(initialCapacity: Int = 0) : CollectionUpdater<E, Set<E>> {
  private val set = ArraySet<E>(initialCapacity)

  override fun add(value: E): Boolean {
    return set.add(value)
  }

  override fun getCollection(): Set<E> {
    return set
  }
}

class LinkedHashSetCollectionUpdater<E>(
    initialCapacity: Int = 16,
    loadFactor: Float = 0.75f
) : CollectionUpdater<E, Set<E>> {
  private val set = LinkedHashSet<E>(initialCapacity, loadFactor)

  override fun add(value: E) = set.add(value)

  override fun getCollection() = set
}

class ImmutableSetCollectionUpdater<E>(expectedSize: Int = 4) : CollectionUpdater<E, Set<E>> {
  private val set = ImmutableSet.builderWithExpectedSize<E>(expectedSize)

  override fun add(value: E): Boolean {
    set.add(value)
    // Guava doesn't indicate prior presence
    return false
  }

  override fun getCollection() = set.build()
}

/** Converts collection types to JSON arrays containing their converted contents.  */
class CustomCollectionJsonAdapter<C : MutableCollection<E>, E> private constructor(
  private val elementAdapter: JsonAdapter<E>,
  private val collectionUpdater: CollectionUpdater.Factory,
  private val toJsonDelegate: JsonAdapter<C>
) : JsonAdapter<C>() {

  override fun fromJson(reader: JsonReader): C {
    val updater = collectionUpdater.create<E, C>()
    reader.beginArray()
    while (reader.hasNext()) {
      updater.add(elementAdapter.fromJson(reader)!!)
    }
    reader.endArray()
    return updater.getCollection()
  }

  override fun toJson(writer: JsonWriter, value: C?) = toJsonDelegate.toJson(writer, value)

  override fun toString(): String {
    return "$elementAdapter.customCollection()"
  }

  companion object {
    @JvmStatic
    fun newFactory(
        listUpdaterFactory: CollectionUpdater.Factory? = null,
        setUpdaterFactory: CollectionUpdater.Factory? = null
    ) = object : Factory {
      override fun create(type: Type, annotations: Set<Annotation>, moshi: Moshi): JsonAdapter<*>? {
        if (annotations.isEmpty()) {
          val rawType = Types.getRawType(type)
          if (setUpdaterFactory != null && rawType.isAssignableFrom(Set::class.java)) {
            return newSetAdapter<Any>(type, moshi, setUpdaterFactory).nullSafe()
          } else if (listUpdaterFactory != null && rawType.isAssignableFrom(Collection::class.java)) {
            return newListAdapter<Any>(type, moshi, listUpdaterFactory).nullSafe()
          }
        }
        return null
      }

      private fun <T> newListAdapter(
          type: Type,
          moshi: Moshi,
          listUpdaterFactory: CollectionUpdater.Factory
      ): JsonAdapter<MutableCollection<T>> {
        val elementType = Types.collectionElementType(type, MutableCollection::class.java)
        val elementAdapter = moshi.adapter<T>(elementType)
        return CustomCollectionJsonAdapter(elementAdapter, listUpdaterFactory, moshi.nextAdapter(this, type, emptySet()))
      }

      private fun <T> newSetAdapter(
          type: Type,
          moshi: Moshi,
          setUpdaterFactory: CollectionUpdater.Factory
      ): JsonAdapter<MutableCollection<T>> {
        val elementType = Types.collectionElementType(type, MutableCollection::class.java)
        val elementAdapter = moshi.adapter<T>(elementType)
        return CustomCollectionJsonAdapter(elementAdapter, setUpdaterFactory, moshi.nextAdapter(this, type, emptySet()))
      }
    }

  }
}
