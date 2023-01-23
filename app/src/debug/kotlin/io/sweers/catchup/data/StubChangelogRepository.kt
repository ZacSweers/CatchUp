package io.sweers.catchup.data

import com.squareup.anvil.annotations.ContributesBinding
import dev.zacsweers.catchup.di.AppScope
import io.sweers.catchup.service.api.CatchUpItem
import io.sweers.catchup.ui.about.ChangelogRepository
import io.sweers.catchup.ui.about.ChangelogRepositoryImpl
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
