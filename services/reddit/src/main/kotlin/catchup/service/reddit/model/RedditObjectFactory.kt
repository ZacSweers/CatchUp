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
package catchup.service.reddit.model

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.lang.reflect.Type

internal class RedditObjectFactory : JsonAdapter.Factory {

  override fun create(
    type: Type,
    annotations: Set<Annotation>,
    moshi: Moshi,
  ): JsonAdapter<RedditObject>? {
    if (!Types.getRawType(type).isAssignableFrom(RedditObject::class.java)) {
      // Not one of our oddball polymorphic types, ignore it.
      return null
    }
    return object : JsonAdapter<RedditObject>() {

      private val kindAdapter = moshi.adapter(RedditKind::class.java)

      override fun fromJson(reader: JsonReader): RedditObject? {
        return when (reader.peek()) {
          JsonReader.Token.STRING -> {
            // It's null or there are no replies, just return null
            reader.skipValue()
            null
          }
          JsonReader.Token.NULL -> reader.nextNull()
          else -> {
            reader.beginObject()
            val kind = reader.peekJson().use(::readKind)
            var data: RedditObject? = null
            while (reader.hasNext()) {
              if (reader.selectName(DATA_OPTIONS) == -1) {
                reader.skipName()
                reader.skipValue()
                continue
              }

              data = moshi.adapter(kind.derivedClass).fromJson(reader) ?: throw JsonDataException()
            }
            reader.endObject()
            data ?: throw JsonDataException("Missing 'data' label!")
          }
        }
      }

      private fun readKind(reader: JsonReader): RedditKind {
        while (reader.hasNext()) {
          if (reader.selectName(KIND_OPTIONS) == -1) {
            reader.skipName()
            reader.skipValue()
            continue
          }
          return kindAdapter.fromJson(reader)
            ?: throw JsonDataException("Unrecognized kind: ${reader.nextString()}")
        }

        throw JsonDataException("Missing 'kind' label!")
      }

      override fun toJson(writer: JsonWriter, value: RedditObject?) {
        when (value) {
          is RedditComment -> writer.write(value)
          is RedditLink -> writer.write(value)
          is RedditListing -> writer.write(value)
          else -> writer.nullValue()
        }
      }

      // TODO still duplicating some here, but reified types DRY this up nicely
      private inline fun <reified T : RedditObject> JsonWriter.write(value: T?) {
        name("kind")
        moshi
          .adapter(RedditKind::class.java)
          .toJson(this, RedditKind.entries.find { it.derivedClass == T::class.java })
        name("data")
        moshi.adapter(T::class.java).toJson(this, value)
      }
    }
  }

  companion object {
    val INSTANCE: RedditObjectFactory by lazy { RedditObjectFactory() }
    private val KIND_OPTIONS = JsonReader.Options.of("kind")
    private val DATA_OPTIONS = JsonReader.Options.of("data")
  }
}
