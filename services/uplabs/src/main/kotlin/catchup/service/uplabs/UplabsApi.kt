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
package catchup.service.uplabs

import catchup.service.uplabs.model.UplabsComments
import catchup.service.uplabs.model.UplabsImage
import retrofit2.http.GET
import retrofit2.http.Query

/** Uplabs API - https://www.uplabs.com/all.json ¯\_(ツ)_/¯ */
interface UplabsApi {

  @GET("/all.json")
  suspend fun getPopular(
    @Query("days_ago") daysAgo: Int, // Default 0
    @Query("page") page: Int, // Default 1
  ): List<UplabsImage>

  @GET("/comments.json")
  suspend fun getComments(
    @Query("commentable_id") id: Long,
    @Query("commentable_type") type: String = "post",
  ): UplabsComments

  companion object {
    private const val HOST = "www.uplabs.com"
    const val ENDPOINT = "https://$HOST"
  }
}
