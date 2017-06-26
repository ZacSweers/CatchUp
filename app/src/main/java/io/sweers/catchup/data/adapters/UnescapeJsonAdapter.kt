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

package io.sweers.catchup.data.adapters

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Types
import io.sweers.catchup.util.unescapeJavaString

/**
 * [JsonAdapter] that defaults the given element if it is a collection to an empty form
 * if it is null, denoted via [UnEscape].
 */
class UnescapeJsonAdapter internal constructor(
    private val delegate: JsonAdapter<String>) : JsonAdapter<String>() {

  override fun fromJson(reader: JsonReader): String? {
    var fromJson: String = delegate.fromJson(reader)!!
    fromJson = fromJson.unescapeJavaString()
    return fromJson
  }

  override fun toJson(writer: JsonWriter, value: String?) {
    delegate.toJson(writer, value)
  }

  override fun toString(): String {
    return "$delegate.unescaping()"
  }

  companion object {
    @JvmField val FACTORY = Factory { type, annotations, moshi ->
      if (annotations.size > 1 || annotations.none { UnEscape::class.java.isInstance(it) }) {
        return@Factory null
      }

      return@Factory UnescapeJsonAdapter(moshi.adapter(type,
          Types.nextAnnotations(annotations, UnEscape::class.java)!!))
    }
  }
}
