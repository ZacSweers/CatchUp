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

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.Exclude
import io.sweers.catchup.service.api.HasStableId
import org.threeten.bp.Instant
import java.util.Locale
import java.util.concurrent.TimeUnit

internal data class HackerNewsStory(
  val by: String,
  val dead: Boolean,
  val deleted: Boolean,
  val descendants: Int,
  val id: Long,
  val kids: List<Long>?,
  val parent: HackerNewsStory?,
  val parts: List<String>?,
  val score: Int,
  private val time: Long?,
  val title: String,
  val text: String?,
  private val type: String,
  val url: String?) : HasStableId {

  @Exclude
  override fun stableId() = id
//
//  class InstantAdapter : TypeAdapter<Instant, Long> {
//    override fun fromFirebaseValue(value: Long?): Instant =
//        Instant.ofEpochMilli(TimeUnit.MILLISECONDS.convert(value!!, TimeUnit.SECONDS))
//
//    override fun toFirebaseValue(value: Instant): Long? {
//      val longTime = value.toEpochMilli()
//      return TimeUnit.MILLISECONDS.convert(longTime, TimeUnit.SECONDS)
//    }
//  }
//
//  class HNTypeAdapter : TypeAdapter<HNType, String> {
//    override fun fromFirebaseValue(value: String): HNType =
//        HNType.valueOf(value.toUpperCase(Locale.US))
//
//    override fun toFirebaseValue(value: HNType): String {
//      return value.name
//          .toLowerCase(Locale.US)
//    }
//  }

  @Exclude
  fun resolveTime(): Instant {
    return Instant.ofEpochMilli(TimeUnit.MILLISECONDS.convert(time!!, TimeUnit.SECONDS))
  }

  @Exclude
  fun resolveType(): HNType {
    return HNType.valueOf(type.toUpperCase(Locale.US))
  }

  companion object {

    fun create(dataSnapshot: DataSnapshot): HackerNewsStory {
      return dataSnapshot.getValue(HackerNewsStory::class.java)!!
    }
  }
}
