package catchup.bookmarks

import catchup.bookmarks.db.BookmarksDatabase
import catchup.di.AppScope
import catchup.di.SingleIn
import catchup.service.db.CatchUpDatabase
import com.squareup.anvil.annotations.ContributesBinding
import javax.inject.Inject

@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
class BookmarkRepositoryBinding @Inject constructor(
  bookmarksDb: BookmarksDatabase,
  catchupDb: CatchUpDatabase) :
  BookmarkRepository by BookmarkRepositoryImpl(bookmarksDb, catchupDb)
