package catchup.bookmarks

import app.cash.sqldelight.Query
import app.cash.sqldelight.Transacter
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import catchup.bookmarks.db.Bookmark
import catchup.bookmarks.db.BookmarksDatabase
import catchup.service.api.CatchUpItem
import catchup.service.api.toCatchUpItem
import catchup.service.db.CatchUpDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock.System
import kotlinx.datetime.Instant

interface BookmarkRepository {
  fun addBookmark(id: Long, timestamp: Instant = System.now())

  fun removeBookmark(id: Long)

  fun isBookmarked(id: Long): Flow<Boolean>

  // Exposed to create a PagingSource
  fun bookmarksCountQuery(): Query<Long>
  fun bookmarksTransacter(): Transacter

  fun bookmarksQuery(limit: Long, offset: Long): Query<CatchUpItem>
}

internal class BookmarkRepositoryImpl(
  private val bookmarksDb: BookmarksDatabase,
  private val catchupDb: CatchUpDatabase
) : BookmarkRepository {
  private val scope = CoroutineScope(Dispatchers.IO)

  // Maintain an in-memory cache of all the bookmarks
  private val bookmarks = MutableStateFlow(LinkedHashSet<Long>())

  init {
    scope.launch {
      val idsFlow =
        bookmarksDb.transactionWithResult { bookmarksDb.bookmarksQueries.bookmarkIds().asFlow() }
      idsFlow.collect { query ->
        // Preserve order
        bookmarks.emit(query.executeAsList().mapTo(LinkedHashSet(), Bookmark::id))
      }
    }
  }

  override fun addBookmark(id: Long, timestamp: Instant) {
    scope.launch { bookmarksDb.transaction { bookmarksDb.bookmarksQueries.addBookmark(id, timestamp) } }
  }

  override fun removeBookmark(id: Long) {
    scope.launch { bookmarksDb.transaction { bookmarksDb.bookmarksQueries.removeBookmark(id) } }
  }

  override fun isBookmarked(id: Long) = bookmarks.map { id in it }

  override fun bookmarksCountQuery() = bookmarksDb.bookmarksQueries.bookmarkedItemsCount()

  override fun bookmarksTransacter(): Transacter = bookmarksDb.bookmarksQueries

  override fun bookmarksQuery(limit: Long, offset: Long): Query<CatchUpItem> {
    val bookmarkIds = bookmarksDb.bookmarksQueries.bookmarkedItems(limit, offset)
    return catchupDb.serviceQueries.itemsByIds(bookmarkIds.executeAsList())
      .map { it.toCatchUpItem() }
  }
}

fun <T : Any, R : Any> Query<T>.map(mapper: (T) -> R): Query<R> {
  @Suppress("UNCHECKED_CAST") return MappedQuery(this, mapper as (Any) -> Any) as Query<R>
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
        val mappedValue = delegate.value?.let {
          if (it is List<*>) {
            it.map {
              it?.let(newMapper)
            }
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
