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
package catchup.util.data.adapters

import catchup.util.parsePossiblyOffsetInstant
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import kotlin.time.Instant

class ISO8601InstantAdapter : JsonAdapter<Instant>() {

  override fun fromJson(reader: JsonReader) = reader.nextString().parsePossiblyOffsetInstant()

  override fun toJson(writer: JsonWriter, instant: Instant?) {
    writer.value(instant!!.toString())
  }
}
