/*
 * Copyright (C) 2019. Zac Sweers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.sweers.catchup.service.imgur

import com.serjltt.moshi.adapters.Wrapped
import io.reactivex.Single
import io.sweers.catchup.service.imgur.model.Image
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Imgur API - https://apidocs.imgur.com/
 */
internal interface ImgurApi {

  // For "front page", use gallery/hot/viral?showMature=false
  @GET("gallery/r/{subreddit}/top?showMature=false&window=day")
  @Wrapped(path = ["data"])
  fun subreddit(
    @Path("subreddit") subreddit: String,
    @Query("page") page: Int
  ): Single<List<Image>>

  companion object {

    const val HOST = "api.imgur.com/3/"
    const val ENDPOINT = "https://$HOST"
  }
}
