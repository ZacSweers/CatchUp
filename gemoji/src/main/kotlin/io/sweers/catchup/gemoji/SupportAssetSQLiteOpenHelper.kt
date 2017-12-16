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
import io.sweers.catchup.gemoji.AssetSQLiteOpenHelper

/**
 * A support version of [AssetSQLiteOpenHelper].
 *
 * Can be hooked up to a Room db by adding an open helper factory to a room database builder.
 * ```
 * builder.openHelperFactory {
 *   SupportAssetSQLiteOpenHelper(it.context, it.name, it.callback.version, it.callback)
 * }
 * ```
 */
internal class SupportAssetSQLiteOpenHelper(context: Context, name: String, version: Int,
    callback: SupportSQLiteOpenHelper.Callback): SupportSQLiteOpenHelper {

  private val wrapper = SQLiteOpenHelperWrapper(context, name, version, callback)

  override fun getDatabaseName(): String {
    return wrapper.databaseName
  }

  override fun setWriteAheadLoggingEnabled(enabled: Boolean) {
    wrapper.setWriteAheadLoggingEnabled(enabled)
  }

  override fun getWritableDatabase(): SupportSQLiteDatabase {
    return wrapper.writableSupportDatabase
  }

  override fun getReadableDatabase(): SupportSQLiteDatabase {
    return wrapper.readableSupportDatabase
  }

  override fun close() {
    wrapper.close()
  }
}

/**
 * Wraps a [SQLiteDatabase] into a [SupportSQLiteDatabase] by using room's internal
 * [FrameworkSQLiteDatabase].
 */
private class SQLiteOpenHelperWrapper(context: Context, name: String, version: Int,
    private val callback: SupportSQLiteOpenHelper.Callback):
    AssetSQLiteOpenHelper(context, name, version) {

  private var wrappedDb: FrameworkSQLiteDatabase? = null

  val writableSupportDatabase: SupportSQLiteDatabase
    @Synchronized get() = wrap(super.getWritableDatabase())

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
