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
package catchup.util.apollo

import catchup.util.parsePossiblyOffsetInstant
import com.apollographql.apollo3.api.Adapter
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonReader.Token.STRING
import com.apollographql.apollo3.api.json.JsonWriter
import kotlinx.datetime.Instant

/** A CustomTypeAdapter for apollo that can convert ISO style date strings to Instant. */
object ISO8601InstantApolloAdapter : Adapter<Instant> {
  override fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): Instant {
    return when (reader.peek()) {
      STRING -> reader.nextString()!!.parsePossiblyOffsetInstant()
      else -> throw IllegalArgumentException("Value wasn't a string!")
    }
  }

  override fun toJson(
    writer: JsonWriter,
    customScalarAdapters: CustomScalarAdapters,
    value: Instant,
  ) {
    writer.value(value.toString())
  }
}
