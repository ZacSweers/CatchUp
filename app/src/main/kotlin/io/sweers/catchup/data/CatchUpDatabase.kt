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

import android.arch.persistence.room.Database
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import android.arch.persistence.room.TypeConverter
import android.arch.persistence.room.TypeConverters
import android.content.Context
import io.sweers.catchup.ui.controllers.SmmryDao
import io.sweers.catchup.ui.controllers.SmmryStorageEntry
import org.threeten.bp.Instant

@Database(entities = arrayOf(
    ServicePage::class,
    CatchUpItem2::class,
    SmmryStorageEntry::class),
    version = 1)
@TypeConverters(CatchUpConverters::class)
abstract class CatchUpDatabase : RoomDatabase() {

  abstract fun serviceDao(): ServiceDao
  abstract fun smmryDao(): SmmryDao

  companion object {

    private var INSTANCE: CatchUpDatabase? = null

    fun getDatabase(context: Context): CatchUpDatabase {
      return INSTANCE ?: Room.databaseBuilder(context.applicationContext,
          CatchUpDatabase::class.java,
          "catchup.db")
          .build()
          .also { INSTANCE = it }
    }
  }
}

internal class CatchUpConverters {

  // Instant
  @TypeConverter
  fun toInstant(timestamp: Long?): Instant? {
    return timestamp?.let { Instant.ofEpochMilli(it) }
  }

  @TypeConverter
  fun toTimestamp(instant: Instant?): Long? {
    return instant?.toEpochMilli()
  }

  // List<Long>
  @TypeConverter
  fun toList(listString: String?): List<Long>? {
    return listString?.let { it.split(",").asSequence().toList().map { it.toLong() } }
  }

  @TypeConverter
  fun toListString(list: List<Long>?): String? {
    return list?.joinToString(",")
  }

  // Pair<String, Int>
  @TypeConverter
  fun toPair(pairString: String?): Pair<String, Int>? {
    return pairString?.let { it.split(",").let { Pair(it[0], it[1].toInt()) } }
  }

  @TypeConverter
  fun toPairString(pair: Pair<String, Int>?): String? {
    return pair?.let { "${pair.first},${pair.second}" }
  }
}
