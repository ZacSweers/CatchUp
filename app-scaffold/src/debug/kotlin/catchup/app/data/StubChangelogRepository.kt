package catchup.app.data

import catchup.app.ui.about.ChangelogRepository
import catchup.app.ui.about.ChangelogRepositoryImpl
import catchup.di.FakeMode
import catchup.service.api.CatchUpItem
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

// TODO https://github.com/ZacSweers/metro/issues/205
// @ContributesBinding(AppScope::class, replaces = [ChangelogRepositoryImpl::class])
// @Inject
class StubChangelogRepository(
  @FakeMode private val isFakeMode: Boolean,
  private val realImpl: Lazy<ChangelogRepositoryImpl>,
) : ChangelogRepository {
  override suspend fun requestItems(): ImmutableList<CatchUpItem> {
    return if (isFakeMode) {
      buildList { repeat(15) { index -> add(CatchUpItem(index.toLong(), "0.1.0")) } }
        .toImmutableList()
    } else {
      TODO()
      //      realImpl.value.requestItems()
    }
  }
}
