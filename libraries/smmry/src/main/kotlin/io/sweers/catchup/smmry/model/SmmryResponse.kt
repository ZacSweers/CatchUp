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
package io.sweers.catchup.smmry.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import dev.zacsweers.moshisealed.annotations.DefaultObject
import dev.zacsweers.moshisealed.annotations.TypeLabel
import io.sweers.catchup.util.data.adapters.UnEscape
import java.io.IOException
import java.lang.ref.WeakReference
import java.lang.reflect.Type
import java.util.Locale

private const val ERROR_KEY = "sm_api_error"
private const val ERROR_MESSAGE = "sm_api_message"

sealed class SmmryResponse {
  @JsonClass(generateAdapter = true)
  data class Success(

      /**
       * Contains the amount of characters returned
       */
      @Json(name = "sm_api_character_count") val characterCount: String,

      /**
       * Contains the title when available
       */
      @Json(name = "sm_api_title")
      @UnEscape val title: String,

      /**
       * Contains the summary
       */
      @Json(name = "sm_api_content") val content: String,

      /**
       * Contains top ranked keywords in descending order
       */
      @Json(name = "sm_api_keyword_array") val keywords: List<String>? = null
  ) : SmmryResponse() {

    companion object {

      fun just(title: String, text: String): Success =
          Success(text.length.toString(), title, text, null)
    }
  }

  @JsonClass(generateAdapter = true, generator = "sealed:$ERROR_KEY")
  sealed class Failure(message: String) : SmmryResponse() {

    val normalizedMessage: String

    init {
      val locale = Locale.getDefault()
      normalizedMessage = message.toLowerCase(locale).capitalize(locale)
    }

    // 0 - Internal server problem which isn't your fault
    @TypeLabel("0")
    @JsonClass(generateAdapter = true)
    data class InternalError(
        @Json(name = ERROR_MESSAGE) val message: String
    ) : Failure("Smmry internal error - $message")

    // 1 - Incorrect submission variables
    @TypeLabel("1")
    @JsonClass(generateAdapter = true)
    data class IncorrectVariables(
        @Json(name = ERROR_MESSAGE) val message: String
    ) : Failure("Smmry invalid input - $message")

    // 2 - Intentional restriction (low credits/disabled API key/banned API key)
    @TypeLabel("2")
    @JsonClass(generateAdapter = true)
    data class ApiRejection(
        @Json(name = ERROR_MESSAGE) val message: String
    ) : Failure("Smmry API error - $message")

    // 3 - Summarization error
    @TypeLabel("3")
    @JsonClass(generateAdapter = true)
    data class SummarizationError(
        @Json(name = ERROR_MESSAGE) val message: String
    ) : Failure("Smmry summarization error - $message")

    @DefaultObject
    object UnknownErrorCode : Failure("Unknown error.")
  }
}


class SmmryResponseFactory : JsonAdapter.Factory {

  override fun create(type: Type, annotations: Set<Annotation>, moshi: Moshi): JsonAdapter<*>? {
    val clazz = Types.getRawType(type)
    if (SmmryResponse::class.java != clazz) {
      return null
    }
    val successAdapter = moshi.adapter(SmmryResponse.Success::class.java)
    val failureAdapter = moshi.adapter(SmmryResponse.Failure::class.java)
    return object : JsonAdapter<SmmryResponse>() {
      @Throws(IOException::class)
      override fun fromJson(reader: JsonReader): SmmryResponse? {
        val jsonValue = reader.readJsonValue()

        @Suppress("UNCHECKED_CAST")
        val value = jsonValue as Map<String, Any>
        return value[ERROR_KEY]?.let {
          failureAdapter.fromJsonValue(value)
        } ?: successAdapter.fromJsonValue(value)
      }

      @Throws(IOException::class)
      override fun toJson(writer: JsonWriter, value: SmmryResponse?) {
        when (value) {
          is SmmryResponse.Success -> successAdapter.toJson(writer, value)
          is SmmryResponse.Failure -> failureAdapter.toJson(writer, value)
        }
      }
    }
  }

  companion object {
    private var instance: WeakReference<SmmryResponseFactory>? = null

    fun getInstance() =
        instance?.get() ?: SmmryResponseFactory().also { instance = WeakReference(it) }
  }
}
