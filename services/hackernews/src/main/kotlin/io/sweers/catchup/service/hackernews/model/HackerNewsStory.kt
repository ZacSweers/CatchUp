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

package io.sweers.catchup.service.hackernews.model

import com.google.auto.value.AutoValue
import com.google.firebase.database.DataSnapshot
import io.sweers.catchup.service.api.HasStableId
import me.mattlogan.auto.value.firebase.adapter.FirebaseAdapter
import me.mattlogan.auto.value.firebase.adapter.TypeAdapter
import me.mattlogan.auto.value.firebase.annotation.FirebaseValue
import org.threeten.bp.Instant
import java.util.Locale
import java.util.concurrent.TimeUnit

@AutoValue
@FirebaseValue
internal abstract class HackerNewsStory : HasStableId {

  abstract fun by(): String

  abstract fun dead(): Boolean

  abstract fun deleted(): Boolean

  abstract fun descendants(): Int

  abstract fun id(): Long

  abstract fun kids(): List<Long>?

  abstract fun parent(): HackerNewsStory?

  abstract fun parts(): List<String>?

  abstract fun score(): Int

  @FirebaseAdapter(InstantAdapter::class) abstract fun time(): Instant

  abstract fun title(): String

  abstract fun text(): String?

  @FirebaseAdapter(
      HNTypeAdapter::class) abstract fun type(): HNType

  abstract fun url(): String?

  override fun stableId() = id()

  class InstantAdapter : TypeAdapter<Instant, Long> {
    override fun fromFirebaseValue(value: Long?): Instant =
        Instant.ofEpochMilli(TimeUnit.MILLISECONDS.convert(value!!, TimeUnit.SECONDS))

    override fun toFirebaseValue(value: Instant): Long? {
      val longTime = value.toEpochMilli()
      return TimeUnit.MILLISECONDS.convert(longTime, TimeUnit.SECONDS)
    }
  }

  class HNTypeAdapter : TypeAdapter<HNType, String> {
    override fun fromFirebaseValue(value: String): HNType =
        HNType.valueOf(value.toUpperCase(Locale.US))

    override fun toFirebaseValue(value: HNType): String {
      return value.name
          .toLowerCase(Locale.US)
    }
  }

  companion object {

    fun create(dataSnapshot: DataSnapshot): HackerNewsStory {
      return dataSnapshot.getValue(AutoValue_HackerNewsStory.FirebaseValue::class.java)!!
          .toAutoValue()
    }
  }
}
