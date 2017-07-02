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

package io.sweers.catchup.data

import android.support.v4.util.Pair
import com.google.auto.value.AutoValue
import io.sweers.catchup.ui.base.HasStableId
import org.threeten.bp.Instant

@AutoValue
abstract class CatchUpItem : HasStableId {

  abstract fun id(): Long

  abstract fun title(): CharSequence

  abstract fun score(): Pair<String, Int>?

  abstract fun timestamp(): Instant

  abstract fun tag(): String?

  abstract fun author(): CharSequence?

  abstract fun source(): CharSequence?

  abstract fun commentCount(): Int

  abstract fun hideComments(): Boolean

  abstract fun itemClickUrl(): String?

  abstract fun itemCommentClickUrl(): String?

  override fun stableId(): Long = id()

  @AutoValue.Builder
  interface Builder {
    fun id(id: Long): Builder

    fun title(title: CharSequence): Builder

    fun score(score: Pair<String, Int>?): Builder

    fun timestamp(timestamp: Instant): Builder

    fun tag(tag: String?): Builder

    fun author(author: CharSequence?): Builder

    fun source(source: CharSequence?): Builder

    fun commentCount(commentCount: Int): Builder

    fun hideComments(hideComments: Boolean): Builder

    fun itemClickUrl(itemClickUrl: String?): Builder

    fun itemCommentClickUrl(itemCommentClickUrl: String?): Builder

    fun build(): CatchUpItem
  }

  companion object {

    fun builder(): Builder {
      return AutoValue_CatchUpItem.Builder()
          .hideComments(false)
          .commentCount(0)
    }
  }
}
