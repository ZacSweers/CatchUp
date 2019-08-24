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
package io.sweers.catchup.smmry.model

import androidx.annotation.Keep
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase

private const val TABLE = "smmryEntries"

@Keep
@Entity(tableName = TABLE)
data class SmmryStorageEntry(
  @PrimaryKey val url: String,
  val json: String
)

@Dao
interface SmmryDao {

  @Query("SELECT * FROM $TABLE WHERE url = :url")
  suspend fun getItem(url: String): SmmryStorageEntry?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun putItem(item: SmmryStorageEntry)

  @Query("DELETE FROM $TABLE")
  fun nukeItems()
}

@Database(
    entities = [SmmryStorageEntry::class],
    version = 1
)
abstract class SmmryDatabase : RoomDatabase() {

  abstract fun dao(): SmmryDao
}
