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

import androidx.annotation.Keep
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import io.sweers.catchup.service.api.CatchUpItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.threeten.bp.Instant

suspend fun ServiceDao.getFirstServicePage(type: String, expiration: Instant) = withContext(Dispatchers.IO) {
  getFirstServicePageBlocking(type, expiration)
}
suspend fun ServiceDao.getFirstServicePage(type: String) = withContext(Dispatchers.IO) {
  getFirstServicePageBlocking(type)
}
suspend fun ServiceDao.getFirstServicePage(type: String, page: String) = withContext(Dispatchers.IO) {
  getFirstServicePageBlocking(type, page)
}
suspend fun ServiceDao.getServicePage(type: String, page: String, sessionId: Long) = withContext(Dispatchers.IO) {
  getServicePageBlocking(type, page, sessionId)
}
suspend fun ServiceDao.getItemById(id: Long) = withContext(Dispatchers.IO) {
  getItemByIdBlocking(id)
}
suspend fun ServiceDao.getItemByIds(ids: Array<Long>) = withContext(Dispatchers.IO) {
  getItemByIdsBlocking(ids)
}
suspend fun ServiceDao.putPage(page: ServicePage) = withContext(Dispatchers.IO) {
  putPageBlocking(page)
}
suspend fun ServiceDao.putItem(item: CatchUpItem) = withContext(Dispatchers.IO) {
  putItemBlocking(item)
}
suspend fun ServiceDao.putItems(vararg items: CatchUpItem) = withContext(Dispatchers.IO) {
  putItemsBlocking(items = *items)
}

@Dao
interface ServiceDao {

  @Query("SELECT * FROM pages WHERE type = :type AND page = 0 AND expiration > :expiration")
  fun getFirstServicePageBlocking(type: String, expiration: Instant): ServicePage?

  @Query("SELECT * FROM pages WHERE type = :type AND page = 0 ORDER BY expiration DESC")
  fun getFirstServicePageBlocking(type: String): ServicePage?

  @Query("SELECT * FROM pages WHERE type = :type AND page = :page ORDER BY expiration DESC")
  fun getFirstServicePageBlocking(type: String, page: String): ServicePage?

  @Query("SELECT * FROM pages WHERE type = :type AND page = :page AND sessionId = :sessionId")
  fun getServicePageBlocking(type: String, page: String, sessionId: Long): ServicePage?

  @Query("SELECT * FROM items WHERE id = :id")
  fun getItemByIdBlocking(id: Long): CatchUpItem?

  @Query("SELECT * FROM items WHERE id IN(:ids)")
  fun getItemByIdsBlocking(ids: Array<Long>): List<CatchUpItem>?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  fun putPageBlocking(page: ServicePage)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  fun putItemBlocking(item: CatchUpItem)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  fun putItemsBlocking(vararg items: CatchUpItem)

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
