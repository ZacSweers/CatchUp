/*
 * Copyright (C) 2014 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.moshi;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import io.sweers.arraysetbackport.ArraySet;

/** Converts collection types to JSON arrays containing their converted contents. */
public abstract class ArrayCollectionJsonAdapter<C extends Collection<T>, T> extends JsonAdapter<C> {
  public static final JsonAdapter.Factory FACTORY = (type, annotations, moshi) -> {
    Class<?> rawType = Types.getRawType(type);
    if (!annotations.isEmpty()) return null;
    if (rawType == List.class || rawType == Collection.class) {
      return newListAdapter(type, moshi).nullSafe();
    } else if (rawType == Set.class) {
      return newSetAdapter(type, moshi).nullSafe();
    }
    return null;
  };

  private final JsonAdapter<T> elementAdapter;

  private ArrayCollectionJsonAdapter(JsonAdapter<T> elementAdapter) {
    this.elementAdapter = elementAdapter;
  }

  static <T> JsonAdapter<Collection<T>> newListAdapter(Type type, Moshi moshi) {
    Type elementType = Types.collectionElementType(type, Collection.class);
    JsonAdapter<T> elementAdapter = moshi.adapter(elementType);
    return new ArrayCollectionJsonAdapter<Collection<T>, T>(elementAdapter) {
      @Override Collection<T> newCollection() {
        return new ArrayList<>();
      }
    };
  }

  static <T> JsonAdapter<Set<T>> newSetAdapter(Type type, Moshi moshi) {
    Type elementType = Types.collectionElementType(type, Collection.class);
    JsonAdapter<T> elementAdapter = moshi.adapter(elementType);
    return new ArrayCollectionJsonAdapter<Set<T>, T>(elementAdapter) {
      @Override Set<T> newCollection() {
        return new ArraySet<>();
      }
    };
  }

  abstract C newCollection();

  @Override public C fromJson(JsonReader reader) throws IOException {
    C result = newCollection();
    reader.beginArray();
    while (reader.hasNext()) {
      result.add(elementAdapter.fromJson(reader));
    }
    reader.endArray();
    return result;
  }

  @Override public void toJson(JsonWriter writer, C value) throws IOException {
    writer.beginArray();
    for (T element : value) {
      elementAdapter.toJson(writer, element);
    }
    writer.endArray();
  }

  @Override public String toString() {
    return elementAdapter + ".collection()";
  }
}
