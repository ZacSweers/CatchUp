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
import io.sweers.catchup.util.data.adapters.UnEscape

@AutoValue
internal abstract class RedditLink : RedditSubmission() {

  abstract fun clicked(): Boolean

  abstract fun domain(): String?

  abstract fun hidden(): Boolean

  @Json(name = "is_self") abstract val isSelf: Boolean

  @Json(name = "link_flair_text") abstract fun linkFlairText(): String?

  @Json(name = "num_comments") abstract fun commentsCount(): Int

  abstract fun permalink(): String

  abstract fun selftext(): String?

  @Json(name = "selftext_html") abstract fun selftextHtml(): String?

  abstract fun stickied(): Boolean

  abstract fun thumbnail(): String

  @UnEscape(html = true) abstract fun title(): String

  abstract fun url(): String

  abstract fun visited(): Boolean

  companion object {
    @JvmStatic
    fun jsonAdapter(moshi: Moshi): JsonAdapter<RedditLink> {
      return AutoValue_RedditLink.MoshiJsonAdapter(moshi)
    }
  }
}
