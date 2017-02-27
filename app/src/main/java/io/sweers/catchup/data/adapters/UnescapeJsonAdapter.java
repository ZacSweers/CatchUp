/*
 * Copyright 2016 Serj Lotutovici
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

package io.sweers.catchup.data.adapters;

import android.support.annotation.Nullable;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import com.squareup.moshi.Types;
import io.sweers.catchup.util.Strings;
import java.io.IOException;
import java.lang.annotation.Annotation;

import static io.sweers.catchup.data.adapters.Util.findAnnotation;

/**
 * {@linkplain JsonAdapter} that defaults the given element if it is a collection to an empty form
 * if it is null, denoted via {@link UnEscape}.
 */
@SuppressWarnings("unchecked")
public final class UnescapeJsonAdapter extends JsonAdapter<String> {
  public static final Factory FACTORY = (type, annotations, moshi) -> {
    Annotation annotation = findAnnotation(annotations, UnEscape.class);
    if (annotation == null || annotations.size() > 1) {
      return null;
    }

    return new UnescapeJsonAdapter(moshi.adapter(type,
        Types.nextAnnotations(annotations, UnEscape.class)));
  };

  private final JsonAdapter<String> delegate;

  UnescapeJsonAdapter(JsonAdapter<String> delegate) {
    this.delegate = delegate;
  }

  @Nullable @Override public String fromJson(JsonReader reader) throws IOException {
    String fromJson = delegate.fromJson(reader);
    fromJson = Strings.unescapeJavaString(fromJson);
    return fromJson;
  }

  @Override public void toJson(JsonWriter writer, String value) throws IOException {
    delegate.toJson(writer, value);
  }

  @Override public String toString() {
    return delegate.toString() + ".unescaping()";
  }
}
