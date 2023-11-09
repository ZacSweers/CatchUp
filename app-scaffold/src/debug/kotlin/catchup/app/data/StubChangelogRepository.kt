package catchup.app.data

import catchup.app.ui.about.ChangelogRepository
import catchup.app.ui.about.ChangelogRepositoryImpl
import catchup.di.AppScope
import catchup.service.api.CatchUpItem
import com.squareup.anvil.annotations.ContributesBinding
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.first

@ContributesBinding(AppScope::class, replaces = [ChangelogRepositoryImpl::class])
class StubChangelogRepository
@Inject
constructor(
  private val debugPreferences: DebugPreferences,
  private val realImpl: dagger.Lazy<ChangelogRepositoryImpl>,
) : ChangelogRepository {
  override suspend fun requestItems(): ImmutableList<CatchUpItem> {
    return if (debugPreferences.mockModeEnabled.first()) {
      buildList { repeat(15) { index -> add(CatchUpItem(index.toLong(), "0.1.0")) } }
        .toImmutableList()
    } else {
      realImpl.get().requestItems()
    }
  }
}
