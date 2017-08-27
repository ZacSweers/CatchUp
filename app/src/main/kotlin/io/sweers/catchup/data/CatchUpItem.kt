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

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Entity
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy.REPLACE
import android.arch.persistence.room.PrimaryKey
import android.arch.persistence.room.Query
import android.support.annotation.Keep
import com.google.auto.value.AutoValue
import io.reactivex.Maybe
import io.sweers.catchup.ui.base.HasStableId
import org.threeten.bp.Instant

@Dao
interface ServiceDao {

  @Query("SELECT * FROM pages WHERE type = :type AND page = 0 AND expiration > :expiration")
  fun getFirstServicePage(type: String, expiration: Instant): Maybe<ServicePage>

  @Query("SELECT * FROM pages WHERE type = :type AND page = 0 ORDER BY expiration DESC")
  fun getFirstServicePage(type: String): Maybe<ServicePage>

  @Query("SELECT * FROM pages WHERE type = :type AND page = :page AND sessionId = :sessionId")
  fun getServicePage(type: String, page: Int, sessionId: Long): Maybe<ServicePage>

  @Query("SELECT * FROM items WHERE id = :id")
  fun getItemById(id: Long): Maybe<CatchUpItem2>

  @Query("SELECT * FROM items WHERE id IN(:ids)")
  fun getItemByIds(ids: Array<Long>): Maybe<List<CatchUpItem2>>

  @Insert(onConflict = REPLACE)
  fun putPage(page: ServicePage)

  @Insert(onConflict = REPLACE)
  fun putItem(item: CatchUpItem2)

  @Insert(onConflict = REPLACE)
  fun putItems(vararg item: CatchUpItem2)

  @Query("DELETE FROM pages")
  fun nukePages()

  @Query("DELETE FROM items")
  fun nukeItems()

}

@Keep
@Entity(tableName = "pages")
data class ServicePage(
    /**
     * Combination of the sessionId and type
     */
    @PrimaryKey val id: String,
    val type: String,
    val expiration: Instant,
    val sessionId: Long = -1,
    val page: Int = 0,
    val items: List<Long>
)

@Keep
@Entity(tableName = "items")
data class CatchUpItem2(
    @PrimaryKey var id: Long,
    val title: String,
    val timestamp: Instant,
    val score: Pair<String, Int>? = null,
    val tag: String? = null,
    val author: String? = null,
    val source: String? = null,
    val commentCount: Int = 0,
    val hideComments: Boolean = false,
    val itemClickUrl: String? = null,
    val itemCommentClickUrl: String? = null
) : HasStableId {
  override fun stableId() = id
}

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

    fun from(item: CatchUpItem2): CatchUpItem {
      return builder()
          .id(item.id)
          .title(item.title)
          .score(item.score)
          .timestamp(item.timestamp)
          .tag(item.tag)
          .author(item.author)
          .source(item.source)
          .commentCount(item.commentCount)
          .hideComments(item.hideComments)
          .itemClickUrl(item.itemClickUrl)
          .itemCommentClickUrl(item.itemCommentClickUrl)
          .build()
    }

    fun builder(): Builder {
      return AutoValue_CatchUpItem.Builder()
          .hideComments(false)
          .commentCount(0)
    }
  }
}
