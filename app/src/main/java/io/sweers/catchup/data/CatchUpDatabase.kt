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
import android.content.Context
import io.sweers.catchup.ui.controllers.SmmryDao
import io.sweers.catchup.ui.controllers.SmmryStorageEntry

@Database(entities = arrayOf(
    ServicePage::class,
    CatchUpItem2::class,
    SmmryStorageEntry::class),
    version = 1)
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
