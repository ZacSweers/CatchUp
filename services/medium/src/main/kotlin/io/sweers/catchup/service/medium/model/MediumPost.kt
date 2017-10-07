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

import io.sweers.catchup.service.api.HasStableId

@AutoValue
internal abstract class MediumPost : HasStableId {

  abstract fun collection(): Collection?

  abstract fun post(): Post

  abstract fun user(): User

  fun constructUrl() = "https://medium.com/@${user().username()}/${post().uniqueSlug()}"

  fun constructCommentsUrl() = "${constructUrl()}#--responses"

  override fun stableId(): Long = post().id().hashCode().toLong()

  @AutoValue.Builder
  abstract class Builder {
    abstract fun collection(value: Collection?): Builder

    abstract fun post(value: Post): Builder

    abstract fun user(value: User): Builder

    abstract fun build(): MediumPost
  }

  companion object {
    @JvmStatic
    fun jsonAdapter(moshi: Moshi): JsonAdapter<MediumPost> =
        AutoValue_MediumPost.MoshiJsonAdapter(moshi)

    // Ew
    fun builder(): Builder = `$AutoValue_MediumPost`.Builder()
  }
}
