/*
 * Copyright 2015 Google Inc.
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

package io.sweers.catchup.data

import com.google.auto.value.AutoValue
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * A [Interceptor] that adds an auth token to requests.
 *
 * Can't be a data class because we want @Redacted.
 */
@AutoValue
abstract class AuthInterceptor : Interceptor {

  @Redacted abstract fun accessToken(): String

  abstract fun method(): String

  @Throws(IOException::class)
  override fun intercept(chain: Interceptor.Chain): Response {
    val request = chain.request()
        .newBuilder()
        .addHeader("Authorization", method() + " " + accessToken())
        .build()
    return chain.proceed(request)
  }

  companion object {

    fun create(method: String, accessToken: String): AuthInterceptor {
      return AutoValue_AuthInterceptor(accessToken, method)
    }
  }
}
