package catchup.app.data

import catchup.app.ui.about.LicensesRepository
import catchup.app.ui.about.LicensesRepositoryImpl
import catchup.app.ui.about.OssBaseItem
import catchup.app.ui.about.OssItem
import catchup.di.FakeMode
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

// TODO https://github.com/ZacSweers/metro/issues/205
// @ContributesBinding(AppScope::class, replaces = [LicensesRepositoryImpl::class])
// @Inject
class StubLicensesRepository(
  @FakeMode private val isFakeMode: Boolean,
  private val realImpl: Lazy<LicensesRepositoryImpl>,
) : LicensesRepository {
  override suspend fun requestItems(): ImmutableList<OssBaseItem> {
    return if (isFakeMode) {
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
                "https://github.com/ZacSweers",
              )
            )
          }
        }
        .toImmutableList()
    } else {
      realImpl.value.requestItems()
    }
  }
}
