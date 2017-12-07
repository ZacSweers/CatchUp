/*
 * Copyright (C) 2016 The Android Open Source Project
 * Modifications (c) 2017 CommonsWare, LLC
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

// Need package to access FrameworkSQLiteDatabase
@file:Suppress("PackageDirectoryMismatch")
package android.arch.persistence.db.framework

import android.arch.persistence.db.SupportSQLiteDatabase
import android.arch.persistence.db.SupportSQLiteOpenHelper
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.readystatesoftware.sqliteasset.SQLiteAssetHelper

internal class AssetSQLiteOpenHelper(context: Context, name: String, version: Int,
    callback: SupportSQLiteOpenHelper.Callback): SupportSQLiteOpenHelper {

  val assetHelper = AssetHelper(context, name, version, callback)

  override fun getDatabaseName(): String {
    return assetHelper.databaseName
  }

  override fun setWriteAheadLoggingEnabled(enabled: Boolean) {
    assetHelper.setWriteAheadLoggingEnabled(enabled)
  }

  override fun getWritableDatabase(): SupportSQLiteDatabase {
    return assetHelper.writableSupportDatabase
  }

  override fun getReadableDatabase(): SupportSQLiteDatabase {
    return assetHelper.readableSupportDatabase
  }

  override fun close() {
    assetHelper.close()
  }
}

internal class AssetHelper(context: Context, name: String, private val version: Int,
    private val callback: SupportSQLiteOpenHelper.Callback):
    SQLiteAssetHelper(context, name, null, null, version, null) {

  private var wrappedDb: FrameworkSQLiteDatabase? = null
  private var updateIdentity = false

  init {
    setForcedUpgrade()
  }

  val writableSupportDatabase: SupportSQLiteDatabase
    @Synchronized get() {
      val database = wrap(super.getWritableDatabase())
      updateIdentity = database.version != version
      return database
    }

  val readableSupportDatabase: SupportSQLiteDatabase
    @Synchronized get() = wrap(super.getReadableDatabase())

  override fun onCreate(sqLiteDatabase: SQLiteDatabase) {
    callback.onCreate(wrap(sqLiteDatabase))
  }

  override fun onUpgrade(sqLiteDatabase: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    callback.onUpgrade(wrap(sqLiteDatabase), oldVersion, newVersion)
  }

  override fun onConfigure(sqLiteDatabase: SQLiteDatabase) {
    callback.onConfigure(wrap(sqLiteDatabase))
  }

  override fun onDowngrade(sqLiteDatabase: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    callback.onDowngrade(wrap(sqLiteDatabase), oldVersion, newVersion)
  }

  override fun onOpen(sqLiteDatabase: SQLiteDatabase) {
    callback.onOpen(wrap(sqLiteDatabase))
  }

  @Synchronized override fun close() {
    super.close()
    wrappedDb = null
  }

  @Synchronized private fun wrap(sqLiteDatabase: SQLiteDatabase): FrameworkSQLiteDatabase {
    if (wrappedDb == null) {
      wrappedDb = FrameworkSQLiteDatabase(sqLiteDatabase)
    }
    return wrappedDb!!
  }
}
