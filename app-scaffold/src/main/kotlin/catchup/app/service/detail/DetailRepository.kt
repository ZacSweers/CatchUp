package catchup.app.service.detail

import catchup.di.ContextualFactory
import catchup.di.DataMode
import catchup.service.api.CatchUpItem
import catchup.service.api.Detail
import catchup.service.api.Service
import catchup.service.api.toCatchUpItem
import catchup.service.db.CatchUpDatabase
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import javax.inject.Provider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import me.saket.unfurl.UnfurlResult
import me.saket.unfurl.Unfurler

class DetailRepository
@AssistedInject
constructor(
  @Assisted private val itemId: Long,
  @Assisted private val serviceId: String,
  private val dbFactory: ContextualFactory<DataMode, out CatchUpDatabase>,
  services: @JvmSuppressWildcards Map<String, Provider<Service>>,
  private val unfurler: Unfurler,
) {

  private val service = services.getValue(serviceId).get()

  // TODO DB caching

  fun loadDetail(): Flow<CompositeDetail> {
    // TODO refresh/updates
    return flow { loadDetailParallel() }
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

  private fun loadUnfurl(linkUrl: String): UnfurlResult? {
    return unfurler.unfurl(linkUrl)
  }

  @AssistedFactory
  fun interface Factory {
    fun create(itemId: Long, serviceId: String): DetailRepository
  }

  data class CompositeDetail(val detail: Detail, val unfurl: UnfurlResult?)
}
