/*
 * Copyright (C) 2026. Zac Sweers
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
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
package catchup.bookmarks

import app.cash.sqldelight.Query
import app.cash.sqldelight.Transacter
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import catchup.bookmarks.db.Bookmark
import catchup.bookmarks.db.CatchUpDatabase
import catchup.service.api.CatchUpItem
import catchup.service.api.toCatchUpItem
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlin.time.Clock.System
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

interface BookmarkRepository {
  suspend fun addBookmark(id: Long, timestamp: Instant = System.now())

  suspend fun removeBookmark(id: Long)

  fun isBookmarked(id: Long): Flow<Boolean>

  fun bookmarksCountFlow(): Flow<Long>

  // Exposed to create a PagingSource
  fun bookmarksCountQuery(): Query<Long>

  fun bookmarksTransacter(): Transacter

  fun bookmarksQuery(limit: Long, offset: Long): Query<CatchUpItem>
}

@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
@Inject
class BookmarkRepositoryImpl(
  private val database: CatchUpDatabase,
  // TODO replace with background scope from DI graph
  scope: CoroutineScope = CoroutineScope(Dispatchers.IO),
) : BookmarkRepository {

  // Maintain an in-memory cache of all the bookmarks
  private val bookmarks = MutableStateFlow<Set<Long>>(emptySet())

  init {
    scope.launch {
      withContext(Dispatchers.IO) {
        database
          .transactionWithResult { database.bookmarksQueries.bookmarkIds().asFlow() }
          .map { it.executeAsList().mapTo(LinkedHashSet(), Bookmark::id) }
          .collect(bookmarks::emit)
      }
    }
  }

  override suspend fun addBookmark(id: Long, timestamp: Instant) {
    withContext(Dispatchers.IO) {
      database.transaction { database.bookmarksQueries.addBookmark(id, timestamp) }
    }
  }

  override suspend fun removeBookmark(id: Long) {
    withContext(Dispatchers.IO) {
      database.transaction { database.bookmarksQueries.removeBookmark(id) }
    }
  }

  override fun isBookmarked(id: Long) = bookmarks.map { id in it }

  override fun bookmarksCountFlow(): Flow<Long> {
    return bookmarksCountQuery().asFlow().map { it.executeAsOne() }.flowOn(Dispatchers.IO)
  }

  override fun bookmarksCountQuery() = database.bookmarksQueries.bookmarkedItemsCount()

  override fun bookmarksTransacter(): Transacter = database.bookmarksQueries

  override fun bookmarksQuery(limit: Long, offset: Long): Query<CatchUpItem> {
    return database.bookmarksQueries.bookmarkedItems(limit, offset).map { it.toCatchUpItem() }
  }
}

fun <T : Any, R : Any> Query<T>.map(mapper: (T) -> R): Query<R> {
  @Suppress("UNCHECKED_CAST")
  return MappedQuery(this, mapper as (Any) -> Any) as Query<R>
}

private class MappedQuery(private val original: Query<Any>, private val newMapper: (Any) -> Any) :
  Query<Any>(original.mapper) {
  override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> {
    return when (val delegate = original.execute(mapper)) {
      is QueryResult.AsyncValue -> {
        @Suppress("UNCHECKED_CAST")
        QueryResult.AsyncValue { delegate.value?.let(newMapper) } as QueryResult<R>
      }
      is QueryResult.Value -> {
        val mappedValue =
          delegate.value?.let {
            if (it is List<*>) {
              it.map { it?.let(newMapper) }
            } else {
              newMapper(it)
            }
          }
        @Suppress("UNCHECKED_CAST")
        QueryResult.Value(mappedValue) as QueryResult<R>
      }
    }
  }

  override fun addListener(listener: Listener) {
    original.addListener(listener)
  }

  override fun removeListener(listener: Listener) {
    original.removeListener(listener)
  }
}
