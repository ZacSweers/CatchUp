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

import app.cash.sqldelight.ColumnAdapter
import app.cash.sqldelight.adapter.primitive.FloatColumnAdapter
import app.cash.sqldelight.adapter.primitive.IntColumnAdapter
import catchup.bookmarks.db.Bookmark
import catchup.bookmarks.db.CatchUpDatabase as BookmarksDatabase
import catchup.di.AppScope
import catchup.di.ContextualFactory
import catchup.di.DataMode
import catchup.di.SingleIn
import catchup.service.db.CatchUpDatabase
import catchup.service.db.CatchUpDbItem
import catchup.sqldelight.SqlDriverFactory
import com.squareup.anvil.annotations.ContributesTo
import dagger.Binds
import dagger.Lazy
import dagger.Module
import dagger.Provides
import kotlinx.datetime.Instant

/**
 * This setup is a little weird but apparently how SqlDelight works.
 *
 * [BookmarksDatabase] is the "real" db instance, but they all implement the same base interface.
 *
 * We expose a [ContextualFactory] for the DBs so that we can switch between real and fake easily.
 */
@ContributesTo(AppScope::class)
@Module
abstract class CatchUpDatabaseModule {

  @Binds
  abstract fun provideCatchUpDbFactory(
    real: ContextualFactory<DataMode, BookmarksDatabase>
  ): ContextualFactory<DataMode, out CatchUpDatabase>

  companion object {
    // Unscoped, the real DB instance is a singleton
    @Provides
    fun provideBookmarksDatabaseFactory(
      factory: SqlDriverFactory,
      realDb: Lazy<BookmarksDatabase>,
    ): ContextualFactory<DataMode, BookmarksDatabase> {
      return ContextualFactory { mode ->
        when (mode) {
          // Fakes are unscoped but that's fine, they're in-memory and whatever
          DataMode.FAKE -> createDb(factory, null)
          else -> realDb.get()
        }
      }
    }

    // Singleton instance of the "real" db
    @Provides
    @SingleIn(AppScope::class)
    fun provideBookmarksDatabase(factory: SqlDriverFactory): BookmarksDatabase =
      createDb(factory, "catchup.db")

    private fun createDb(factory: SqlDriverFactory, dbName: String?): BookmarksDatabase {
      return BookmarksDatabase(
        factory.create(BookmarksDatabase.Schema, dbName),
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
}

object InstantColumnAdapter : ColumnAdapter<Instant, Long> {
  override fun decode(databaseValue: Long) = Instant.fromEpochMilliseconds(databaseValue)

  override fun encode(value: Instant) = value.toEpochMilliseconds()
}

fun CatchUpDatabase.lastUpdated(serviceId: String): Long? {
  return serviceQueries.lastOperation(serviceId).executeAsOneOrNull()?.timestamp
}
