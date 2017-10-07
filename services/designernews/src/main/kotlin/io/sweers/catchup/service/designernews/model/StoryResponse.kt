/*
 * Copyright (c) 2017 Zac Sweers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.sweers.catchup.service.designernews.model

import com.google.auto.value.AutoValue
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi

/**
 * Models a response from the Designer News API that returns a single story
 */
@AutoValue
internal abstract class StoryResponse {

  abstract fun story(): Story

  companion object {

    @JvmStatic
    fun jsonAdapter(moshi: Moshi): JsonAdapter<StoryResponse> =
        AutoValue_StoryResponse.MoshiJsonAdapter(moshi)
  }
}
