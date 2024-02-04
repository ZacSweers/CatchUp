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
package catchup.service.github

import catchup.service.github.model.TrendingItem
import java.util.Locale
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/** Fake API for https://github.com/trending */
interface GitHubApi {

  @GET("/trending{$LANGUAGE}")
  suspend fun getTrending(
    @Path(LANGUAGE) language: Language,
    @Query("since") since: Since,
  ): List<TrendingItem>

  @Suppress("unused")
  enum class Since {
    DAILY,
    WEEKLY,
    MONTHLY;

    override fun toString(): String {
      return super.toString().lowercase(Locale.US)
    }
  }

  @Suppress("unused")
  sealed class Language {
    object All : Language() {
      override fun toString(): String {
        return ""
      }
    }

    data class Custom(val name: String) : Language() {
      override fun toString(): String {
        return "/${name.lowercase(Locale.US)}"
      }
    }
  }

  companion object {

    private const val LANGUAGE = "language"
    private const val HOST = "github.com"
    const val ENDPOINT = "https://$HOST"
  }
}
