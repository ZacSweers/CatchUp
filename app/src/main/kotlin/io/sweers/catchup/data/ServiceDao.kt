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
package io.sweers.catchup.data

import androidx.annotation.Keep
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import io.reactivex.Completable
import io.reactivex.Maybe
import io.sweers.catchup.service.api.CatchUpItem
import org.threeten.bp.Instant

@Dao
interface ServiceDao {

  @Query("SELECT * FROM pages WHERE type = :type AND page = 0 AND expiration > :expiration")
  fun getFirstServicePage(type: String, expiration: Instant): Maybe<ServicePage>

  @Query("SELECT * FROM pages WHERE type = :type AND page = 0 ORDER BY expiration DESC")
  fun getFirstServicePage(type: String): Maybe<ServicePage>

  @Query("SELECT * FROM pages WHERE type = :type AND page = :page ORDER BY expiration DESC")
  fun getFirstServicePage(type: String, page: String): Maybe<ServicePage>

  @Query("SELECT * FROM pages WHERE type = :type AND page = :page AND sessionId = :sessionId")
  fun getServicePage(type: String, page: String, sessionId: Long): Maybe<ServicePage>

  @Query("SELECT * FROM items WHERE id = :id")
  fun getItemById(id: Long): Maybe<CatchUpItem>

  @Query("SELECT * FROM items WHERE id IN(:ids)")
  fun getItemByIds(ids: Array<Long>): Maybe<List<CatchUpItem>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  fun putPage(page: ServicePage): Completable

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  fun putItem(item: CatchUpItem)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  fun putItems(vararg item: CatchUpItem): Completable

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
  val page: String,
  val sessionId: Long = -1,
  val items: List<Long>,
  val nextPageToken: String?
)
