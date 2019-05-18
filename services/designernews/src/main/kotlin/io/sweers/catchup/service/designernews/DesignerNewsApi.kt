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
package io.sweers.catchup.service.designernews

import com.serjltt.moshi.adapters.Wrapped
import io.reactivex.Single
import io.sweers.catchup.service.designernews.model.Story
import io.sweers.catchup.service.designernews.model.User
import io.sweers.catchup.util.collect.CommaJoinerList
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Models the Designer News API.
 *
 * v2 docs: https://github.com/DesignerNews/dn_api_v2
 */
internal interface DesignerNewsApi {

  @GET("stories")
  @Wrapped(path = ["stories"])
  fun getTopStories(@Query("page") page: Int): Single<List<Story>>

  @GET("users/{ids}")
  @Wrapped(path = ["users"])
  fun getUsers(@Path("ids") ids: CommaJoinerList<String>): Single<List<User>>

  companion object {

    const val HOST = "www.designernews.co/api/v2/"
    const val ENDPOINT = "https://$HOST"
  }
}
