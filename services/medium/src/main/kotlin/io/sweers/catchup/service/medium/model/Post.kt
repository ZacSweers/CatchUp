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

package io.sweers.catchup.service.medium.model

import com.google.auto.value.AutoValue
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi

import org.threeten.bp.Instant

@AutoValue
internal abstract class Post {

  abstract fun createdAt(): Instant

  abstract fun creatorId(): String

  abstract fun homeCollectionId(): String

  abstract fun id(): String

  abstract fun title(): String

  abstract fun uniqueSlug(): String

  abstract fun virtuals(): Virtuals

  companion object {
    @JvmStatic
    fun jsonAdapter(moshi: Moshi): JsonAdapter<Post> = AutoValue_Post.MoshiJsonAdapter(moshi)
  }
}
