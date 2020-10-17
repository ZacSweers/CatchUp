package io.sweers.catchup.ui.fragments.service.v2

import androidx.paging.Pager
import androidx.paging.PagingConfig
import dagger.hilt.android.scopes.ActivityScoped
import io.sweers.catchup.data.CatchUpDatabase
import io.sweers.catchup.injection.DaggerMap
import io.sweers.catchup.service.api.Service
import io.sweers.catchup.service.api.ServiceMeta
import io.sweers.catchup.ui.activity.FinalServices
import javax.inject.Inject
import javax.inject.Provider

/**
 * Repository implementation that uses a database backed [androidx.paging.PagingSource] and
 * [androidx.paging.RemoteMediator] to load pages from network when there are no more items cached
 * in the database to load.
 */
@ActivityScoped
class CatchUpItemRepository @Inject constructor(
  private val db: CatchUpDatabase,
  private val serviceMetas: DaggerMap<String, ServiceMeta>,
  @FinalServices private val services: DaggerMap<String, Provider<Service>>
) {

  fun postsOfService(serviceId: String, pageSize: Int) = Pager(
    config = PagingConfig(pageSize),
    remoteMediator = PageKeyedRemoteMediator(db, services.getValue(serviceId).get(), serviceId)
  ) {
    db.serviceDao().itemsByService(serviceId)
  }.flow

  fun serviceMetaFor(serviceId: String): ServiceMeta = serviceMetas.getValue(serviceId)
}