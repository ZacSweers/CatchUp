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

import com.squareup.moshi.Json
import io.sweers.catchup.service.api.HasStableId
import org.threeten.bp.Instant

internal abstract class RedditSubmission : RedditObject(), HasStableId {

  abstract fun author(): String

  @Json(name = "author_flair_text") abstract fun authorFlairText(): String?

  @Json(name = "banned_by") abstract fun bannedBy(): String?

  abstract fun created(): Instant

  @Json(name = "created_utc") abstract fun createdUtc(): Instant

  abstract fun gilded(): Int

  abstract fun id(): String

  abstract fun name(): String

  abstract fun saved(): Boolean

  abstract fun score(): Int

  abstract fun subreddit(): String

  abstract fun ups(): Int

  override fun stableId(): Long = id().hashCode().toLong()
}
