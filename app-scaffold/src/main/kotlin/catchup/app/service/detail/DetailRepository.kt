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
package catchup.app.service.detail

import catchup.di.ContextualFactory
import catchup.di.DataMode
import catchup.service.api.CatchUpItem
import catchup.service.api.Detail
import catchup.service.api.Service
import catchup.service.api.toCatchUpItem
import catchup.service.db.CatchUpDatabase
import catchup.unfurler.UnfurlResult
import catchup.unfurler.UnfurlerRepository
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

@AssistedInject
class DetailRepository(
  @Assisted private val itemId: Long,
  @Assisted private val serviceId: String,
  private val dbFactory: ContextualFactory<DataMode, out CatchUpDatabase>,
  services: Map<String, () -> Service>,
  private val unfurlerRepository: UnfurlerRepository,
) {

  private val service = services.getValue(serviceId).invoke()

  // TODO DB caching

  fun loadDetail(): Flow<CompositeDetail> {
    // TODO refresh/updates
    return flow { emit(loadDetailParallel()) }
  }

  private suspend fun loadDetailParallel(): CompositeDetail =
    withContext(Dispatchers.IO) {
      val db = dbFactory.create(DataMode.REAL)
      val item = db.serviceQueries.getItem(itemId).executeAsOneOrNull()!!.toCatchUpItem()

      val detail = async { loadDetail(item) }

      val unfurl = async { item.clickUrl?.let { loadUnfurl(it) } }
      CompositeDetail(detail.await(), unfurl.await())
    }

  private suspend fun loadDetail(item: CatchUpItem): Detail {
    return service.fetchDetail(item, item.detailKey!!)
  }

  private suspend fun loadUnfurl(linkUrl: String): UnfurlResult? {
    return unfurlerRepository.loadUnfurl(linkUrl)
  }

  @AssistedFactory
  fun interface Factory {
    fun create(itemId: Long, serviceId: String): DetailRepository
  }

  data class CompositeDetail(val detail: Detail, val unfurl: UnfurlResult?)
}
