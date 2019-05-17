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
package io.sweers.catchup.service.newsapi

import io.reactivex.Single
import io.sweers.catchup.service.newsapi.model.NewsApiResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Models the NewsApi API. See https://newsapi.org/docs/get-started
 */
internal interface NewsApiApi {

  @GET("/v2/everything")
  fun getStories(
    @Query("pageSize") pageSize: Int,
    @Query("page") page: Int,
    @Query("from") from: String,
    @Query("q") query: String,
    @Query("language") language: Language,
    @Query("sortBy") sortBy: SortBy
  ): Single<NewsApiResponse>

  companion object {

    private const val SCHEME = "https"
    const val HOST = "newsapi.org"
    const val ENDPOINT = "$SCHEME://$HOST"
  }
}

internal enum class Language {
  EN;

  override fun toString() = super.toString().toLowerCase()
}

internal enum class SortBy {
  POPULARITY;

  override fun toString() = super.toString().toLowerCase()
}
