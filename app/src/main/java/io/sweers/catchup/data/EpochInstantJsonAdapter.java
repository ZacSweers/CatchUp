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
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.threeten.bp.Instant;

/**
 * Formats dates in UTC seconds or milliseconds time to {@link Instant} instances.
 */
public final class EpochInstantJsonAdapter extends JsonAdapter<Instant> {

  private final TimeUnit timeUnit;

  public EpochInstantJsonAdapter() {
    this(TimeUnit.SECONDS);
  }

  /**
   * @param timeUnit because some APIs give you UTC time in different units
   */
  public EpochInstantJsonAdapter(TimeUnit timeUnit) {
    this.timeUnit = timeUnit;
  }

  @Override public synchronized Instant fromJson(JsonReader reader) throws IOException {
    long l = reader.nextLong();
    return Instant.ofEpochMilli(TimeUnit.MILLISECONDS.convert(l, timeUnit));
  }

  @Override public synchronized void toJson(JsonWriter writer, Instant value) throws IOException {
    long longTime = value.toEpochMilli();
    writer.value(TimeUnit.MILLISECONDS.convert(longTime, timeUnit));
  }
}
