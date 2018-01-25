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

import com.squareup.moshi.Json
import io.sweers.inspector.Inspector
import io.sweers.inspector.ValidationException
import io.sweers.inspector.Validator
import io.sweers.moshkt.api.MoshiSerializable

@MoshiSerializable
internal data class References(
    @Json(name = "Collection") val collection: Map<String, Collection>,
    @Json(name = "Post") val post: Map<String, Post>,
    @Json(name = "User") val user: Map<String, User>) {

  companion object {

    @Suppress("UNUSED_PARAMETER") // Remove when inspector supports 0 arg
    @JvmStatic
    fun validator(inspector: Inspector): Validator<References> {
      return object : Validator<References>() {
        override fun validate(references: References) {
          references.post.values.forEach {
            if (it.creatorId !in references.user) {
              throw ValidationException("Medium Post ${it.id} creator not in user map.")
            }
          }
        }
      }.nullSafe()
    }
  }
}
