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
package io.sweers.catchup.service.medium

import com.serjltt.moshi.adapters.Wrapped
import io.reactivex.Observable
import io.sweers.catchup.service.medium.model.References
import retrofit2.http.GET

internal interface MediumApi {

  @GET("/topic/popular")
  @Wrapped(path = ["payload", "references"])
  fun top(): Observable<References>

  companion object {

    const val HOST = "medium.com"
    const val ENDPOINT = "https://$HOST"
  }
}
