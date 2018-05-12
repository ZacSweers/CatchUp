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

package io.sweers.catchup.service.newsapi.model

import com.squareup.moshi.Json
import io.sweers.catchup.service.newsapi.model.ErrorResponse.ApiKeyDisabled
import io.sweers.catchup.service.newsapi.model.ErrorResponse.ApiKeyExhausted
import io.sweers.catchup.service.newsapi.model.ErrorResponse.ApiKeyInvalid
import io.sweers.catchup.service.newsapi.model.ErrorResponse.ApiKeyMissing
import io.sweers.catchup.service.newsapi.model.ErrorResponse.ParameterInvalid
import io.sweers.catchup.service.newsapi.model.ErrorResponse.ParametersMissing
import io.sweers.catchup.service.newsapi.model.ErrorResponse.RateLimited
import io.sweers.catchup.service.newsapi.model.ErrorResponse.SourceDoesNotExist
import io.sweers.catchup.service.newsapi.model.ErrorResponse.SourcesTooMany
import io.sweers.catchup.service.newsapi.model.ErrorResponse.UnexpectedError
import kotlin.reflect.KClass

internal enum class ErrorCode constructor(val derivedClass: KClass<out ErrorResponse>) {
  @Json(name = "apiKeyDisabled")
  API_KEY_DISABLED(ApiKeyDisabled::class),
  @Json(name = "apiKeyExhausted")
  API_KEY_EXHAUSTED(ApiKeyExhausted::class),
  @Json(name = "apiKeyInvalid")
  API_KEY_INVALID(ApiKeyInvalid::class),
  @Json(name = "apiKeyMissing")
  API_KEY_MISSING(ApiKeyMissing::class),
  @Json(name = "parameterInvalid")
  PARAMETER_INVALID(ParameterInvalid::class),
  @Json(name = "parametersMissing")
  PARAMETERS_MISSING(ParametersMissing::class),
  @Json(name = "rateLimited")
  RATE_LIMITED(RateLimited::class),
  @Json(name = "sourcesTooMany")
  SOURCES_TOO_MANY(SourcesTooMany::class),
  @Json(name = "sourceDoesNotExist")
  SOURCE_DOES_NOT_EXIST(SourceDoesNotExist::class),
  @Json(name = "unexpectedError")
  UNEXPECTED_ERROR(UnexpectedError::class)
}
