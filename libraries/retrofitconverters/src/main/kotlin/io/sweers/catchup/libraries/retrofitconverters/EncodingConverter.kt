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

package io.sweers.catchup.libraries.retrofitconverters

import okhttp3.RequestBody
import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.reflect.Type

/**
 * A [Converter] that only encodes requests with a given [convertBody].
 */
class EncodingConverter<T> private constructor(
    private val convertBody: (T) -> RequestBody) : Converter<T, RequestBody> {


  override fun convert(value: T): RequestBody {
    return convertBody(value)
  }

  companion object {
    /** Creates a new factory for creating converter. We only care about encoding requests. */
    fun <T> newFactory(encoder: (T) -> RequestBody): Converter.Factory {
      return object : Converter.Factory() {
        private val converter by lazy { EncodingConverter(encoder) }
        override fun requestBodyConverter(type: Type, parameterAnnotations: Array<out Annotation>,
            methodAnnotations: Array<out Annotation>,
            retrofit: Retrofit): Converter<*, RequestBody>? {
          return converter
        }
      }
    }
  }

}
