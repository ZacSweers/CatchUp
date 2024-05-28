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
package catchup.util.network

import java.io.IOException
import kotlin.annotation.AnnotationRetention.BINARY
import kotlin.annotation.AnnotationTarget.PROPERTY
import okhttp3.Interceptor
import okhttp3.Response

@Retention(BINARY) @Target(AnnotationTarget.CLASS, PROPERTY) annotation class Redacted

/** A [Interceptor] that adds an auth token to requests. */
// TODO testing K2 plugin IDE support
// @Redacted
data class AuthInterceptor(private val method: String, @Redacted private val accessToken: String) :
  Interceptor {

  @Throws(IOException::class)
  override fun intercept(chain: Interceptor.Chain): Response {
    val request =
      chain.request().newBuilder().addHeader("Authorization", "$method $accessToken").build()
    return chain.proceed(request)
  }
}
