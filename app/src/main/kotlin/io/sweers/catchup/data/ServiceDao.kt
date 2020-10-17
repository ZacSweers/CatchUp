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

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.sweers.catchup.service.api.CatchUpItem

@Dao
interface ServiceDao {

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(posts: List<CatchUpItem>)

  @Query("SELECT * FROM items WHERE serviceId = :serviceId ORDER BY indexInResponse ASC")
  fun itemsByService(serviceId: String): PagingSource<Int, CatchUpItem>

  @Query("DELETE FROM items WHERE serviceId = :serviceId")
  suspend fun deleteByService(serviceId: String)

  @Query("SELECT MAX(indexInResponse) + 1 FROM items WHERE serviceId = :serviceId")
  suspend fun getNextIndexInService(serviceId: String): Int
}
