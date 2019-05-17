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
package io.sweers.catchup.service.reddit

import io.reactivex.Single
import io.sweers.catchup.service.reddit.model.RedditResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

internal interface RedditApi {

  @GET("/r/{subreddit}/comments/{id}")
  fun comments(
    @Path("subreddit") subreddit: String,
    @Path("id") id: String
  ): Single<List<RedditResponse>>

  @GET("/")
  fun frontPage(
    @Query("limit") limit: Int,
    @Query("after") after: String?
  ): Single<RedditResponse>

  @GET("/r/{subreddit}")
  fun subreddit(
    @Path("subreddit") subreddit: String,
    @Query("after") after: String,
    @Query("limit") limit: Int
  ): Single<RedditResponse>

  @GET("/top")
  fun top(
    @Query("after") after: String,
    @Query("limit") limit: Int
  ): Single<RedditResponse>

  companion object {
    const val HOST = "www.reddit.com"
    const val ENDPOINT = "https://$HOST"
  }
}
