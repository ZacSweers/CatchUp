package io.sweers.catchup.service.newsapi.model

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonAdapter.Factory
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import io.sweers.catchup.service.newsapi.model.Status.ERROR
import io.sweers.catchup.service.newsapi.model.Status.OK
import java.io.IOException
import java.lang.reflect.Type

class NewsApiResponseFactory : Factory {

  override fun create(type: Type, annotations: Set<Annotation>, moshi: Moshi): JsonAdapter<*>? {
    val clazz = Types.getRawType(type)
    if (NewsApiResponse::class.java != clazz) {
      return null
    }
    return object : JsonAdapter<NewsApiResponse>() {
      @Throws(IOException::class)
      override fun fromJson(reader: JsonReader): NewsApiResponse? {
        val jsonValue = reader.readJsonValue()

        @Suppress("UNCHECKED_CAST")
        val value = jsonValue as Map<String, Any>
        value["status"]?.let {
          val status = moshi.adapter(Status::class.java).fromJson("\"${it as String}\"")
              ?: throw JsonDataException("Invalid status received: $it")
          return when (status) {
            OK -> moshi.adapter(
                Success::class.java).fromJsonValue(value)
            ERROR -> moshi.adapter(value["code"]
                ?.let {
                  moshi.adapter(ErrorCode::class.java).fromJson("\"${it as String}\"")
                }?.derivedClass?.java
                ?: throw JsonDataException(
                    "No error code received for error status!"))
                .fromJsonValue(value)
          }
        } ?: throw JsonDataException("No status received!")
      }

      @Throws(IOException::class)
      override fun toJson(writer: JsonWriter, value: NewsApiResponse?) {
        when (value) {
          is ErrorResponse -> {
            @Suppress("UNCHECKED_CAST")
            val adapter = moshi.adapter(value.code.derivedClass.java) as JsonAdapter<ErrorResponse>
            adapter.toJson(writer, value)
          }
          is Success -> {
            moshi.adapter(Success::class.java).toJson(writer, value)
          }
        }
      }
    }
  }
}
