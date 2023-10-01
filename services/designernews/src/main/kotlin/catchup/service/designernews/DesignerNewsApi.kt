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
package catchup.service.designernews

import catchup.service.designernews.model.Story
import com.serjltt.moshi.adapters.Wrapped
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Models the Designer News API.
 *
 * v2 docs: https://github.com/DesignerNews/dn_api_v2
 */
interface DesignerNewsApi {

  @GET("stories")
  @Wrapped(path = ["stories"])
  suspend fun getTopStories(@Query("page") page: Int): List<Story>

  companion object {

    private const val HOST = "www.designernews.co/api/v2/"
    const val ENDPOINT = "https://$HOST"
  }
}
