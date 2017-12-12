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

package io.sweers.catchup.gemoji

import android.arch.persistence.db.framework.SupportAssetSQLiteOpenHelper
import android.arch.persistence.room.Database
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import android.content.Context

fun createGemojiDatabase(context: Context, name: String): GemojiDatabase {
  return Room.databaseBuilder(context, GemojiDatabase::class.java, name)
      .openHelperFactory {
        SupportAssetSQLiteOpenHelper(it.context, it.name!!, it.callback.version, it.callback)
      }
      .build()
}

@Database(entities = arrayOf(Gemoji::class), version = 1)
abstract class GemojiDatabase: RoomDatabase() {
  abstract fun GemojiDao(): GemojiDao
}
