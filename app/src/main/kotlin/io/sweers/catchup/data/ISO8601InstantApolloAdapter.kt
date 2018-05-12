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

package io.sweers.catchup.data

import com.apollographql.apollo.response.CustomTypeAdapter
import com.apollographql.apollo.response.CustomTypeValue
import com.apollographql.apollo.response.CustomTypeValue.GraphQLString
import io.sweers.catchup.util.parsePossiblyOffsetInstant
import org.threeten.bp.Instant

/**
 * A CustomTypeAdapter for apollo that can convert ISO style date strings to Instant.
 */
class ISO8601InstantApolloAdapter : CustomTypeAdapter<Instant> {
  override fun decode(value: CustomTypeValue<*>): Instant {
    if (value is GraphQLString) {
      return value.value.parsePossiblyOffsetInstant()
    } else throw IllegalArgumentException("Value is not a string!")
  }

  override fun encode(instant: Instant): CustomTypeValue<*> {
    return GraphQLString.fromRawValue(instant.toString())
  }
}
