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

package io.sweers.catchup.data.reddit.model

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.io.IOException
import java.lang.ref.WeakReference
import java.lang.reflect.Type

class RedditObjectFactory : JsonAdapter.Factory {

  override fun create(type: Type, annotations: Set<Annotation>, moshi: Moshi): JsonAdapter<*>? {
    val clazz = Types.getRawType(type)
    if (RedditObject::class.java != clazz) {
      // Not one of our oddball polymorphic types, ignore it.
      return null
    }
    return object : JsonAdapter<Any>() {
      @Throws(IOException::class)
      override fun fromJson(reader: JsonReader): Any? {
        val jsonValue = reader.readJsonValue()
        if (jsonValue is String) {
          // There are no replies.
          return jsonValue // Or null, or something interesting to you.
        }

        @Suppress("UNCHECKED_CAST")
        val value = jsonValue as Map<String, Any>?
        val redditType = RedditType.valueOf((value!!["kind"] as String).toUpperCase())
        val redditObject = value["data"]
        val adapter = moshi.adapter(redditType.derivedClass)
        if (adapter == null) {
          throw JsonDataException()
        } else {
          return adapter.fromJsonValue(redditObject)
        }
      }

      @Throws(IOException::class)
      override fun toJson(writer: JsonWriter, value: Any?) {
        when (value) {
          is RedditComment -> {
            writer.name("kind")
            moshi.adapter(RedditType::class.java).toJson(writer, RedditType.T1)
            writer.name("data")
            moshi.adapter(RedditComment::class.java).toJson(writer, value)
          }
          is RedditLink -> {
            writer.name("kind")
            moshi.adapter(RedditType::class.java).toJson(writer, RedditType.T3)
            writer.name("data")
            moshi.adapter(RedditLink::class.java).toJson(writer, value)
          }
          is RedditListing -> {
            writer.name("kind")
            moshi.adapter(RedditType::class.java).toJson(writer, RedditType.LISTING)
            writer.name("data")
            moshi.adapter(RedditListing::class.java).toJson(writer, value)
          }
          is RedditMore -> {
            writer.name("kind")
            moshi.adapter(RedditType::class.java).toJson(writer, RedditType.MORE)
            writer.name("data")
            moshi.adapter(RedditMore::class.java).toJson(writer, value)
          }
          else -> TODO()
        }
      }
    }
  }

  companion object {
    private var instance: WeakReference<RedditObjectFactory>? = null

    fun getInstance(): RedditObjectFactory {
      return instance?.get() ?: RedditObjectFactory().apply { instance = WeakReference(this) }
    }
  }
}
