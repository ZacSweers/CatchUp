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

package io.sweers.catchup.data.medium.model

import com.google.auto.value.AutoValue
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi

@AutoValue
abstract class Virtuals {

  abstract fun recommends(): Int

  abstract fun responsesCreatedCount(): Int

  companion object {
    @JvmStatic
    fun jsonAdapter(moshi: Moshi): JsonAdapter<Virtuals> {
      return AutoValue_Virtuals.MoshiJsonAdapter(moshi)
    }
  }
}
