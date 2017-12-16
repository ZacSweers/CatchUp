/*
 * Copyright (C) 2011 readyState Software Ltd, 2007 The Android Open Source Project
 * Modifications (c) 2017 Zac Sweers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.sweers.catchup.gemoji

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.io.File
import java.io.FileOutputStream

/**
 * A minimal implementation of an [SQLiteOpenHelper] that is backed by an sqlite db from
 * `/assets/databases/`.
 *
 * This implementation does not support proper migrations. Instead, when a new version
 * is detected the database from `/assets/databases/` is re-copied into the internal database
 * directory.
 *
 * @param context the context.
 * @param name the db name, the db should be placed under `/assets/databases/name`.
 * @param version the db version, when a new version is detected, the asset db is re-copied into
 * the internal database directory.
 */
internal abstract class AssetSQLiteOpenHelper(private val context: Context,
    name: String,
    private val version: Int) : SQLiteOpenHelper(context, name, null, version) {

  private val databasePath = "${context.applicationInfo.dataDir}/databases/$name"
  private val assetPath = "databases/$name"

  private var database: SQLiteDatabase? = null
    set(value) {
      if (field?.isOpen == true) {
        field?.close()
      }
      field = value
    }

  @Synchronized override fun getWritableDatabase(): SQLiteDatabase {
    database?.let {
      if (it.isOpen && !it.isReadOnly) {
        return@getWritableDatabase it
      }
    }

    var success = false
    var db: SQLiteDatabase? = null
    try {
      if (!databaseExists()) {
        db = createDatabaseFromAssets(version)
      } else {
        db = openDatabase()
        if (db.version < version) {
          db = createDatabaseFromAssets(version)
        }
      }

      onOpen(db)
      success = true
      return db
    } finally {
      if (success) {
        database = db
      } else {
        db?.close()
      }
    }
  }

  @Synchronized override fun getReadableDatabase(): SQLiteDatabase {
    database?.let {
      if (it.isOpen) {
        return@getReadableDatabase it
      }
    }

    return try {
      writableDatabase
    } catch (e: Exception) {
      openDatabase(SQLiteDatabase.OPEN_READONLY).also {
        onOpen(it)
        database = it
      }
    }
  }

  @Synchronized override fun close() {
    database = null
  }

  private fun databaseExists() = File(databasePath).exists()

  private fun openDatabase(flags: Int = SQLiteDatabase.OPEN_READWRITE): SQLiteDatabase {
    return SQLiteDatabase.openDatabase(databasePath, null, flags)
  }

  private fun createDatabaseFromAssets(version: Int): SQLiteDatabase {
    val input = context.assets.open(assetPath)
    val output = FileOutputStream(databasePath)

    try {
      input.copyTo(output)
    } finally {
      input.close()
      output.close()
    }

    val db = openDatabase()
    db.version = version
    onCreate(db)
    return db
  }
}
