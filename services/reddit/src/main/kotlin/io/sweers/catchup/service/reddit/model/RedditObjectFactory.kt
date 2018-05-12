/*
 * Copyright (c) 2018 Zac Sweers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.sweers.catchup.service.reddit.model

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.lang.reflect.Type

internal class RedditObjectFactory : JsonAdapter.Factory {

  override fun create(
      type: Type, annotations: Set<Annotation>, moshi: Moshi): JsonAdapter<RedditObject>? {
    if (!Types.getRawType(type).isAssignableFrom(RedditObject::class.java)) {
      // Not one of our oddball polymorphic types, ignore it.
      return null
    }
    return object : JsonAdapter<RedditObject>() {

      override fun fromJson(reader: JsonReader): RedditObject? {
        val jsonValue = reader.readJsonValue()
        if (jsonValue is String || jsonValue == null) {
          // It's null or there are no replies, just return null
          return null
        }

        @Suppress("UNCHECKED_CAST")
        val value = jsonValue as Map<String, Any>
        val redditType = RedditType.valueOf((value["kind"] as String).toUpperCase())
        val redditObject = value["data"]
        return with(moshi.adapter(redditType.derivedClass)) {
          fromJsonValue(redditObject)
        } ?: throw JsonDataException()
      }

      override fun toJson(writer: JsonWriter, value: RedditObject?) {
        when (value) {
          is RedditComment -> writer.write(value)
          is RedditLink -> writer.write(value)
          is RedditListing -> writer.write(value)
        }
      }

      // TODO still duplicating some here, but reified types DRY this up nicely
      private inline fun <reified T : RedditObject> JsonWriter.write(value: T?) {
        name("kind")
        moshi.adapter(RedditType::class.java)
            .toJson(this, RedditType.values().find { it.derivedClass == T::class.java })
        name("data")
        moshi.adapter(T::class.java).toJson(this, value)
      }
    }
  }

  companion object {
    val INSTANCE: RedditObjectFactory by lazy { RedditObjectFactory() }
  }
}
