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

import io.sweers.catchup.smmry.BuildConfig
import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import java.util.LinkedHashMap

class SmmryRequestBuilder private constructor() {

  // the webpage to summarize
  private var url: String? = null

  // the number of sentences returned, default is 7
  private var sentenceCount: Long = -1

  // how many of the top keywords to return
  private var keywordCount: Long = -1

  // Whether or not to include quotations
  private var avoidQuote = false
  private var avoidQuoteWasSet = false

  // summary will contain string [BREAK] between each sentence
  private var withBreak = false
  private var withBreakWasSet = false

  fun sentenceCount(sentenceCount: Long): SmmryRequestBuilder {
    this.sentenceCount = sentenceCount
    return this
  }

  fun keywordCount(keywordCount: Long): SmmryRequestBuilder {
    this.keywordCount = keywordCount
    return this
  }

  fun avoidQuote(avoidQuote: Boolean): SmmryRequestBuilder {
    this.avoidQuote = avoidQuote
    avoidQuoteWasSet = true
    return this
  }

  fun withBreak(withBreak: Boolean): SmmryRequestBuilder {
    this.withBreak = withBreak
    withBreakWasSet = true
    return this
  }

  fun build(): Map<String, Any> {
    val map = LinkedHashMap<String, Any>(6)
    map["SM_API_KEY"] = BuildConfig.SMMRY_API_KEY
    if (sentenceCount != -1L) {
      map["SM_LENGTH"] = sentenceCount
    }
    if (keywordCount != -1L) {
      map["SM_KEYWORD_COUNT"] = keywordCount
    }
    if (avoidQuoteWasSet) {
      map["SM_QUOTE_AVOID"] = avoidQuote
    }
    if (withBreakWasSet) {
      map["SM_WITH_BREAK"] = withBreak
    }

    // This has to be last!
    url?.run {
      map.put("SM_URL", this)
    }
    return map
  }

  companion object {

    fun forUrl(url: String, isAlreadyEncoded: Boolean = false): SmmryRequestBuilder {
      val builder = SmmryRequestBuilder()
      try {
        builder.url = if (isAlreadyEncoded) url else URLEncoder.encode(url, "UTF-8")
      } catch (e: UnsupportedEncodingException) {
        throw RuntimeException("Invalid url - $url")
      }

      return builder
    }

    fun forText(): SmmryRequestBuilder {
      return SmmryRequestBuilder()
    }
  }
}
