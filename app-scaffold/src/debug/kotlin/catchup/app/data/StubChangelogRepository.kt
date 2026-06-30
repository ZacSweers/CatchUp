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
package catchup.app.data

import catchup.app.ui.about.ChangelogRepository
import catchup.app.ui.about.ChangelogRepositoryImpl
import catchup.di.FakeMode
import catchup.service.api.CatchUpItem
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

@ContributesBinding(AppScope::class, replaces = [ChangelogRepositoryImpl::class])
@Inject
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
