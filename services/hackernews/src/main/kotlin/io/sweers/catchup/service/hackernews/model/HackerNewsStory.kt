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
package io.sweers.catchup.service.hackernews.model

import androidx.annotation.Keep
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.Exclude
import io.sweers.catchup.service.api.HasStableId
import org.threeten.bp.Instant
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.annotation.AnnotationRetention.SOURCE
import kotlin.annotation.AnnotationTarget.CLASS

@Retention(SOURCE)
@Target(CLASS)
annotation class NoArg

@Keep
@NoArg
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
    /* private, but Firebase is too dumb to read private fields */
  val time: Long?,
  val title: String,
  val text: String?,
    /* private, but Firebase is too dumb to read private fields */
  val type: String?,
  val url: String?
) : HasStableId {

  @Exclude
  override fun stableId() = id

  /*
   * Excluded "real" fields. Would like to expose these as the main fields, but firebase matches property names to them anyway
   *
   * They also have to be functions because if you try to read them as fields, they always return null! ¯\_(ツ)_/¯
   */

  @Exclude
  fun realTime(): Instant = time?.let {
    Instant.ofEpochMilli(
        TimeUnit.MILLISECONDS.convert(it, TimeUnit.SECONDS))
  } ?: Instant.now()

  @Exclude
  fun realType() = type?.let { HNType.valueOf(it.toUpperCase(Locale.US)) }

  companion object {

    fun create(dataSnapshot: DataSnapshot): HackerNewsStory {
      return dataSnapshot.getValue(HackerNewsStory::class.java)!!
    }
  }
}

// DataSnapshot { key = 20516882, value = {parent=20516063, by=frou_dh, id=20516882, text=stuff, time=1563986039, type=comment} }
@Keep
@NoArg
internal data class HackerNewsComment(
  val by: String,
//  val dead: Boolean,
  val deleted: Boolean,
//  val descendants: Int,
  val id: Long,
  val kids: List<Long>?,
  val parent: Long?,
//  val parts: List<String>?,
//  val score: Int,
    /* private, but Firebase is too dumb to read private fields */
  val time: Long?,
//  val title: String?,
  val text: String,
    /* private, but Firebase is too dumb to read private fields */
  val type: String?
//  val url: String?
) : HasStableId {

  @Exclude
  override fun stableId() = id

  /*
   * Excluded "real" fields. Would like to expose these as the main fields, but firebase matches property names to them anyway
   *
   * They also have to be functions because if you try to read them as fields, they always return null! ¯\_(ツ)_/¯
   */

  @Exclude
  fun realTime(): Instant = time?.let {
    Instant.ofEpochMilli(
        TimeUnit.MILLISECONDS.convert(it, TimeUnit.SECONDS))
  } ?: Instant.now()

  @Exclude
  fun realType() = type?.let { HNType.valueOf(it.toUpperCase(Locale.US)) }

  companion object {

    fun create(dataSnapshot: DataSnapshot): HackerNewsComment {
      return dataSnapshot.getValue(HackerNewsComment::class.java)!!
    }
  }
}
