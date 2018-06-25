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

package io.sweers.catchup.service.medium.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import io.sweers.inspector.Inspector
import io.sweers.inspector.SelfValidating
import io.sweers.inspector.ValidationException

@JsonClass(generateAdapter = true)
internal data class References(
    @Json(name = "Collection") val collection: Map<String, Collection>?,
    @Json(name = "Post") val post: Map<String, Post>,
    @Json(name = "User") val user: Map<String, User>) : SelfValidating {

  override fun validate(inspector: Inspector) {
    post.values.forEach {
      if (it.creatorId !in user) {
        throw ValidationException("Medium Post ${it.id} creator not in user map.")
      }
    }
  }

}
