package catchup.app.data

import catchup.app.ui.about.ChangelogRepository
import catchup.app.ui.about.ChangelogRepositoryImpl
import catchup.di.AppScope
import catchup.di.FakeMode
import catchup.service.api.CatchUpItem
import com.squareup.anvil.annotations.ContributesBinding
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

@ContributesBinding(AppScope::class, replaces = [ChangelogRepositoryImpl::class])
class StubChangelogRepository
@Inject
constructor(
  @FakeMode private val isFakeMode: Boolean,
  private val realImpl: dagger.Lazy<ChangelogRepositoryImpl>,
) : ChangelogRepository {
  override suspend fun requestItems(): ImmutableList<CatchUpItem> {
    return if (isFakeMode) {
      buildList { repeat(15) { index -> add(CatchUpItem(index.toLong(), "0.1.0")) } }
        .toImmutableList()
    } else {
      realImpl.get().requestItems()
    }
  }
}
