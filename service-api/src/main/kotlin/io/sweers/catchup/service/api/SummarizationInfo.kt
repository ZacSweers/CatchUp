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

package io.sweers.catchup.service.api

import io.sweers.catchup.service.api.SummarizationType.TEXT
import io.sweers.catchup.service.api.SummarizationType.URL
import okhttp3.HttpUrl

class SummarizationInfo(
    val value: String,
    val type: SummarizationType
) {

  companion object {
    fun from(url: String?, text: String? = null): SummarizationInfo? {
      return url?.let {
        if (canSummarize(url, text)) {
          SummarizationInfo(text ?: url, text?.let { TEXT } ?: URL)
        } else null
      }
    }

    /**
     * Really shallow sanity check
     */
    private fun canSummarize(url: String, text: String? = null): Boolean {
      text?.let {
        if (it.isBlank()) {
          return false
        } else if (it.length < 100) {
          return false
        }
      }

      if (url.endsWith(".png")
          || url.endsWith(".gifv")
          || url.endsWith(".jpg")
          || url.endsWith(".jpeg")) {
        return false
      }

      HttpUrl.parse(url)?.let {
        it.host().let {
          if (it.contains("imgur")
              || it.contains("streamable")
              || it.contains("gfycat")
              || it.contains("i.reddit")
              || it.contains("v.reddit")
              || it.contains("twitter.com")
              || it.contains("t.co")
              || it.contains("youtube")
              || it.contains("youtu.be"))
            return false
        }
      }

      return true
    }
  }
}
