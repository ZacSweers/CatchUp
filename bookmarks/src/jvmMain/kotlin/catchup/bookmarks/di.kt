package catchup.bookmarks

import catchup.bookmarks.db.CatchUpDatabase
import catchup.di.AppScope
import catchup.di.SingleIn
import com.squareup.anvil.annotations.ContributesBinding
import javax.inject.Inject

@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
class BookmarkRepositoryBinding @Inject constructor(private val database: CatchUpDatabase) :
  BookmarkRepository by BookmarkRepositoryImpl(database)
