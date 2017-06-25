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

package io.sweers.catchup.data.reddit.model

import com.google.auto.value.AutoValue
import com.squareup.moshi.Json
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi

@AutoValue
abstract class RedditAccount : RedditObject() {

  @Json(name = "comment_karma") abstract fun commentKarma(): Int

  @Json(name = "has_mail") abstract fun hasMail(): Boolean

  @Json(name = "has_mod_mail") abstract fun hasModMail(): Boolean

  @Json(name = "has_verified_email") abstract fun hasVerifiedEmail(): Boolean

  abstract fun id(): String

  @get:Json(name = "is_friend") abstract val isFriend: Boolean

  @get:Json(name = "is_gold") abstract val isGold: Boolean

  @get:Json(name = "is_mod") abstract val isMod: Boolean

  @Json(name = "link_karma") abstract fun linkKarma(): Int

  abstract fun modhash(): String

  abstract fun name(): String

  @Json(name = "over_18") abstract fun nsfw(): Boolean

  companion object {
    @JvmStatic
    fun jsonAdapter(moshi: Moshi): JsonAdapter<RedditAccount> {
      return AutoValue_RedditAccount.MoshiJsonAdapter(moshi)
    }
  }
}
