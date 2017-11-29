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

package io.sweers.catchup.data.gemoji

import android.arch.persistence.db.SupportSQLiteDatabase
import android.arch.persistence.room.Database
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import io.sweers.catchup.R
import okio.Okio

fun GemojiDatabase(context: Context, name: String, moshi: Moshi,
    builder: (RoomDatabase.Builder<GemojiDatabase>.() -> Unit)? = null): GemojiDatabase {

  return Room.databaseBuilder(context, GemojiDatabase::class.java, name)
      .apply { builder?.invoke(this) }
      .addCallback(object: RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
          // Insert into the SQLiteDatabase because we don't have
          // access to the Room implementation yet and trying to access it
          // from this callback results in recursive calling.
          db.beginTransaction()
          try {
            getGemojisFromJson(context, moshi)
                .map { (alias, emoji) ->
                  ContentValues()
                      .apply {
                        put(GEMOJI_ALIAS_COLUMN_NAME, alias)
                        put(GEMOJI_EMOJI_COLUMN_NAME, emoji)
                      }
                }
                .forEach {
                  db.insert(GEMOJI_TABLE_NAME, SQLiteDatabase.CONFLICT_NONE, it)
                }
            db.setTransactionSuccessful()
          } finally {
            db.endTransaction()
          }
        }
      })
      .build()
}

private fun getGemojisFromJson(context: Context, moshi: Moshi): List<Gemoji> {
  val raw = context.resources.openRawResource(R.raw.gemoji)
  val source = Okio.buffer(Okio.source(raw))

  val type = Types.newParameterizedType(List::class.java, GemojiJson::class.java)
  return moshi.adapter<List<GemojiJson>>(type)
      .fromJson(source)
      ?.mapNotNull { (emoji, aliases) ->
        if (emoji != null && aliases != null) {
          emoji to aliases.filterNotNull()
        } else {
          null
        }
      }
      ?.flatMap { (emoji, aliases) ->
        aliases
            .map { alias ->
              Gemoji(alias, emoji)
            }
      }.orEmpty()
}

private data class GemojiJson(
    val emoji: String?,
    val aliases: List<String?>?
)

@Database(entities = arrayOf(Gemoji::class), version = 1)
abstract class GemojiDatabase: RoomDatabase() {
  abstract fun GemojiDao(): GemojiDao
}
