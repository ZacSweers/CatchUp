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
package io.sweers.catchup.libraries.retrofitconverters

import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import java.io.IOException
import java.lang.reflect.Type

/**
 * A [Converter] that only decodes responses with a given [convertBody].
 */
class DecodingConverter<T> private constructor(
  private val convertBody: (ResponseBody) -> T
) : Converter<ResponseBody, T> {

  @Throws(IOException::class)
  override fun convert(value: ResponseBody): T {
    return convertBody(value)
  }

  companion object {
    /** Creates a new factory for creating converter. We only care about decoding responses. */
    fun <T> newFactory(decoder: (ResponseBody) -> T): Converter.Factory {
      return object : Converter.Factory() {
        private val converter by lazy { DecodingConverter(decoder) }
        override fun responseBodyConverter(
          type: Type,
          annotations: Array<Annotation>,
          retrofit: Retrofit
        ) = converter
      }
    }
  }
}
