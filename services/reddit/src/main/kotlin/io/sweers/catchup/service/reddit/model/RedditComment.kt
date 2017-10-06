/*
 * Copyright (c) 2017 Zac Sweers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.sweers.catchup.service.reddit.model

import com.google.auto.value.AutoValue
import com.squareup.moshi.Json
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi

@AutoValue
internal abstract class RedditComment : RedditSubmission() {

  abstract fun body(): String

  @Json(name = "body_html") abstract fun bodyHtml(): String

  abstract fun controversiality(): Int

  abstract fun depth(): Int

  @Json(name = "link_id") abstract fun linkId(): String

  @Json(name = "parent_id") abstract fun parentId(): String

  /**
   * Ugh-asaurus
   *
   * @return list of comments. Or false. Because yeah.
   */
  abstract fun replies(): RedditObject

  @Json(name = "subreddit_id") abstract fun subredditId(): String

  companion object {
    @JvmStatic
    fun jsonAdapter(moshi: Moshi): JsonAdapter<RedditComment> {
      return AutoValue_RedditComment.MoshiJsonAdapter(moshi)
    }
  }
}
