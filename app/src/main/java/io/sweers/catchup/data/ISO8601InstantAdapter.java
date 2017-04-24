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

package io.sweers.catchup.data;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import io.sweers.catchup.util.Instants;
import java.io.IOException;
import org.threeten.bp.Instant;

public final class ISO8601InstantAdapter extends JsonAdapter<Instant> {

  @Override public Instant fromJson(JsonReader reader) throws IOException {
    return Instants.parsePossiblyOffsetInstant(reader.nextString());
  }

  @Override public void toJson(JsonWriter writer, Instant instant) throws IOException {
    writer.value(instant.toString());
  }
}
