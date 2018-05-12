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

import com.squareup.moshi.JsonClass
import io.sweers.catchup.service.api.HasStableId

@JsonClass(generateAdapter = true)
internal data class MediumPost(val collection: Collection?,
    val post: Post,
    val user: User) : HasStableId {

  fun constructUrl() = "https://medium.com/@${user.username}/${post.uniqueSlug}"

  fun constructCommentsUrl() = "${constructUrl()}#--responses"

  override fun stableId(): Long = post.id.hashCode().toLong()

}
