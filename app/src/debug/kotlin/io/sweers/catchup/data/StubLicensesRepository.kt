package io.sweers.catchup.data

import com.squareup.anvil.annotations.ContributesBinding
import catchup.di.AppScope
import io.sweers.catchup.ui.about.LicensesRepository
import io.sweers.catchup.ui.about.LicensesRepositoryImpl
import io.sweers.catchup.ui.about.OssBaseItem
import io.sweers.catchup.ui.about.OssItem
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.first

@ContributesBinding(AppScope::class, replaces = [LicensesRepositoryImpl::class])
class StubLicensesRepository
@Inject
constructor(
  private val debugPreferences: DebugPreferences,
  private val realImpl: dagger.Lazy<LicensesRepositoryImpl>,
) : LicensesRepository {
  override suspend fun requestItems(): ImmutableList<OssBaseItem> {
    return if (debugPreferences.mockModeEnabled.first()) {
      buildList {
          repeat(15) { index ->
            add(
              OssItem(
                "https://example.com/image.png",
                "ZacSweers",
                "MoshiX-$index",
                "Apache v2",
                "https://github.com/ZacSweers/MoshiX",
                "Extra serialization tools for Moshi",
                "https://github.com/ZacSweers"
              )
            )
          }
        }
        .toImmutableList()
    } else {
      realImpl.get().requestItems()
    }
  }
}
