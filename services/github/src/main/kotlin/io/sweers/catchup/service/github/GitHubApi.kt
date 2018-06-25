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

package io.sweers.catchup.service.github

import io.reactivex.Single
import io.sweers.catchup.service.github.model.TrendingItem
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.Locale

/**
 * Fake API for https://github.com/trending
 */
internal interface GitHubApi {

  @GET("/trending/{$LANGUAGE}")
  fun getTrending(
      @Path(LANGUAGE) language: Language,
      @Query("since") since: Since): Single<List<TrendingItem>>

  enum class Since {
    DAILY, WEEKLY, MONTHLY;

    override fun toString(): String {
      return super.toString().toLowerCase()
    }
  }

  sealed class Language {
    object All : Language() {
      override fun toString(): String {
        return "all"
      }
    }

    data class Custom(val name: String) : Language() {
      override fun toString(): String {
        return name.toLowerCase(Locale.US)
      }
    }
  }

  companion object {

    private const val LANGUAGE = "language"
    const val HOST = "github.com"
    const val ENDPOINT = "https://$HOST"
  }

}
