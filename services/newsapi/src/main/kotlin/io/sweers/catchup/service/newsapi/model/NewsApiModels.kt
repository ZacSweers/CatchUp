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

import com.squareup.moshi.JsonClass
import io.sweers.catchup.service.newsapi.model.Status.ERROR
import io.sweers.catchup.service.newsapi.model.Status.OK

/**
 * Base class for all News Api responses
 */
internal sealed class NewsApiResponse(val status: Status)

@JsonClass(generateAdapter = true)
internal data class Success(
    val totalResults: Int,
    val articles: List<Article>) : NewsApiResponse(OK)

internal sealed class ErrorResponse : NewsApiResponse(ERROR) {
  abstract val code: ErrorCode
  abstract val message: String

  @JsonClass(generateAdapter = true)
  internal data class MissingApiKey(
      override val code: ErrorCode,
      override val message: String) : ErrorResponse()

  /**
   * Your API key has been disabled.
   */
  @JsonClass(generateAdapter = true)
  internal data class ApiKeyDisabled(
      override val code: ErrorCode,
      override val message: String) : ErrorResponse()

  /**
   * Your API key has no more requests available.
   */
  @JsonClass(generateAdapter = true)
  internal data class ApiKeyExhausted(
      override val code: ErrorCode,
      override val message: String) : ErrorResponse()

  /**
   * Your API key hasn't been entered correctly. Double check it and try again.
   */
  @JsonClass(generateAdapter = true)
  internal data class ApiKeyInvalid(
      override val code: ErrorCode,
      override val message: String) : ErrorResponse()

  /**
   * Your API key is missing from the request. Append it to the request with one of these methods.
   */
  @JsonClass(generateAdapter = true)
  internal data class ApiKeyMissing(
      override val code: ErrorCode,
      override val message: String) : ErrorResponse()

  /**
   * You've included a parameter in your request which is currently not supported. Check the message property for more details.
   */
  @JsonClass(generateAdapter = true)
  internal data class ParameterInvalid(
      override val code: ErrorCode,
      override val message: String) : ErrorResponse()

  /**
   * Required parameters are missing from the request and it cannot be completed. Check the message property for more details.
   */
  @JsonClass(generateAdapter = true)
  internal data class ParametersMissing(
      override val code: ErrorCode,
      override val message: String) : ErrorResponse()

  /**
   * You have been rate limited. Back off for a while before trying the request again.
   */
  @JsonClass(generateAdapter = true)
  internal data class RateLimited(
      override val code: ErrorCode,
      override val message: String) : ErrorResponse()

  /**
   * You have requested too many sources in a single request. Try splitting the request into 2 smaller requests.
   */
  @JsonClass(generateAdapter = true)
  internal data class SourcesTooMany(
      override val code: ErrorCode,
      override val message: String) : ErrorResponse()

  /**
   * You have requested a source which does not exist.
   */
  @JsonClass(generateAdapter = true)
  internal data class SourceDoesNotExist(
      override val code: ErrorCode,
      override val message: String) : ErrorResponse()

  /**
   * This shouldn't happen, and if it does then it's our fault, not yours. Try the request again shortly.
   */
  @JsonClass(generateAdapter = true)
  internal data class UnexpectedError(
      override val code: ErrorCode,
      override val message: String) : ErrorResponse()
}
