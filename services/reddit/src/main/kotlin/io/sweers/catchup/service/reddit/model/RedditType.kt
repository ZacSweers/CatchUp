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

package io.sweers.catchup.service.reddit.model

import com.squareup.moshi.Json

/**
 * A subset of reddit types used by CatchUp.
 */
internal enum class RedditType constructor(val derivedClass: Class<out RedditObject>) {

  @Json(name = "t1")
  T1(RedditComment::class.java),

  @Json(name = "t3")
  T3(RedditLink::class.java),

  @Json(name = "Listing")
  LISTING(RedditListing::class.java),

}
