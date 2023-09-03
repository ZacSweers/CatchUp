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
package catchup.service.dribbble

import catchup.service.dribbble.model.Shot
import retrofit2.http.GET
import retrofit2.http.Query

/** Dribbble API - http://developer.dribbble.com/v1/ */
interface DribbbleApi {

  @GET("/shots")
  suspend fun getPopular(@Query("page") page: Int, @Query("per_page") pageSize: Int): List<Shot>

  // list=...
  // sort=...
  // timeframe=...

  companion object {

    private const val HOST = "dribbble.com"
    const val ENDPOINT = "https://$HOST"
  }
}
