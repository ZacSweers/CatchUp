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

package io.sweers.catchup.data.reddit.model;

import android.support.annotation.Nullable;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonDataException;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.ref.WeakReference;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Set;

public final class RedditObjectFactory implements JsonAdapter.Factory {
  private static WeakReference<RedditObjectFactory> instance;

  public static RedditObjectFactory getInstance() {
    RedditObjectFactory factory;
    if (instance == null || (factory = instance.get()) == null) {
      factory = new RedditObjectFactory();
      instance = new WeakReference<>(factory);
    }
    return factory;
  }

  @Nullable @Override
  public JsonAdapter<?> create(Type type, Set<? extends Annotation> annotations, Moshi moshi) {
    Class<?> clazz = Types.getRawType(type);
    if (!RedditObject.class.equals(clazz)) {
      // Not one of our oddball polymorphic types, ignore it.
      return null;
    }
    return new JsonAdapter<Object>() {
      @Override public Object fromJson(JsonReader reader) throws IOException {
        Object jsonValue = reader.readJsonValue();
        if (jsonValue instanceof String) {
          // There are no replies.
          return jsonValue; // Or null, or something interesting to you.
        }
        // noinspection unchecked
        Map<String, Object> value = (Map<String, Object>) jsonValue;
        RedditType type = RedditType.valueOf(((String) value.get("kind")).toUpperCase());
        Object redditObject = value.get("data");
        JsonAdapter<?> adapter = moshi.adapter(type.getDerivedClass());
        if (adapter == null) {
          throw new JsonDataException();
        } else {
          return adapter.fromJsonValue(redditObject);
        }
      }

      @Override public void toJson(JsonWriter writer, Object value) throws IOException {
        throw new UnsupportedOperationException("TODO");
        // Check the runtime type of value and delegate.
      }
    };
  }
}
