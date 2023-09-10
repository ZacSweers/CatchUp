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
package catchup.app.data

import android.content.Context
import app.cash.sqldelight.ColumnAdapter
import app.cash.sqldelight.adapter.primitive.FloatColumnAdapter
import app.cash.sqldelight.adapter.primitive.IntColumnAdapter
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import catchup.bookmarks.db.Bookmark
import catchup.bookmarks.db.CatchUpDatabase as BookmarksDatabase
import catchup.di.AppScope
import catchup.di.SingleIn
import catchup.service.db.CatchUpDatabase
import catchup.service.db.CatchUpDbItem
import catchup.util.injection.qualifiers.ApplicationContext
import com.squareup.anvil.annotations.ContributesTo
import dagger.Binds
import dagger.Module
import dagger.Provides
import kotlinx.datetime.Instant

/**
 * This setup is a little weird but apparently how SqlDelight works.
 *
 * [BookmarksDatabase] is the "real" db instance, but they all implement the same base interface.
 */
@ContributesTo(AppScope::class)
@Module
abstract class CatchUpDatabaseModule {
  @Binds
  @SingleIn(AppScope::class)
  abstract fun provideCatchUpDatabase(real: BookmarksDatabase): CatchUpDatabase

  companion object {
    @Provides
    @SingleIn(AppScope::class)
    fun provideBookmarksDatabase(@ApplicationContext context: Context): BookmarksDatabase =
      BookmarksDatabase(
        AndroidSqliteDriver(BookmarksDatabase.Schema, context, "catchup.db"),
        Bookmark.Adapter(InstantColumnAdapter),
        CatchUpDbItem.Adapter(
          InstantColumnAdapter,
          IntColumnAdapter,
          IntColumnAdapter,
          IntColumnAdapter,
          IntColumnAdapter,
          IntColumnAdapter,
          FloatColumnAdapter,
          IntColumnAdapter,
        ),
      )
  }
}

object InstantColumnAdapter : ColumnAdapter<Instant, Long> {
  override fun decode(databaseValue: Long) = Instant.fromEpochMilliseconds(databaseValue)

  override fun encode(value: Instant) = value.toEpochMilliseconds()
}

fun CatchUpDatabase.lastUpdated(serviceId: String): Long? {
  return serviceQueries.lastOperation(serviceId).executeAsOneOrNull()?.timestamp
}
