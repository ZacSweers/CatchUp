package io.sweers.catchup.gemoji

import androidx.sqlite.db.SupportSQLiteOpenHelper

/** Implementation of [SupportSQLiteOpenHelper.Factory] that creates [SQLiteCopyOpenHelper]. */
internal class SQLiteCopyOpenHelperFactory(
  private val copyFromAssetPath: String,
  private val delegate: SupportSQLiteOpenHelper.Factory
) : SupportSQLiteOpenHelper.Factory {
  override fun create(
    configuration: SupportSQLiteOpenHelper.Configuration
  ): SupportSQLiteOpenHelper {
    return SQLiteCopyOpenHelper(
      configuration.context,
      copyFromAssetPath,
      configuration.callback.version,
      delegate.create(configuration)
    )
  }
}
