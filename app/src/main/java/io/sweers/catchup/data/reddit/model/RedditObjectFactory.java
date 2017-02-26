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
import java.util.Collections;
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
        JsonAdapter<Object> adapter = moshi.nextAdapter(RedditObjectFactory.this,
            type.getDerivedClass(),
            Collections.emptySet());
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
