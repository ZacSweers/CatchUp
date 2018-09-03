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

package io.sweers.catchup.service.unsplash

import io.reactivex.Single
import io.sweers.catchup.service.unsplash.model.UnsplashPhoto
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Unsplash API - https://unsplash.com/documentation
 */
internal interface UnsplashApi {

  @GET("/photos/curated")
  fun getPhotos(
      @Query("page") page: Int = 1, // Default 1
      @Query("per_page") pageSize: Int = 10, // Default 10
      @Query("order_by") orderBy: OrderBy = OrderBy.LATEST // latest
  ): Single<List<UnsplashPhoto>>

  companion object {
    const val HOST = "api.unsplash.com"
    const val ENDPOINT = "https://$HOST"
  }

  enum class OrderBy {
    LATEST, OLDEST, POPULAR;

    override fun toString(): String {
      return super.toString().toLowerCase()
    }
  }

}
