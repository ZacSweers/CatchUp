package dev.zacsweers.catchup.service

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.LoadType.REFRESH
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import io.reactivex.rxjava3.core.Flowable
import io.sweers.catchup.data.CatchUpDatabase
import io.sweers.catchup.data.OperationJournalEntry
import io.sweers.catchup.data.RemoteKeyDao
import io.sweers.catchup.data.ServiceDao
import io.sweers.catchup.data.ServiceRemoteKey
import io.sweers.catchup.data.lastUpdated
import io.sweers.catchup.service.api.CatchUpItem
import io.sweers.catchup.service.api.DataRequest
import io.sweers.catchup.service.api.Service
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import timber.log.Timber

@OptIn(ExperimentalPagingApi::class)
class ServiceMediator
@AssistedInject
constructor(
  @Assisted private val service: Service,
  private val catchUpDatabase: CatchUpDatabase,
  private val serviceDao: ServiceDao,
  private val remoteKeyDao: RemoteKeyDao,
  private val contentTypeChecker: ContentTypeChecker
) : RemoteMediator<Int, CatchUpItem>() {

  @AssistedFactory
  fun interface Factory {
    fun create(service: Service): ServiceMediator
  }

  private val serviceId: String = service.meta().id

  override suspend fun load(
    loadType: LoadType,
    state: PagingState<Int, CatchUpItem>
  ): MediatorResult {
    Timber.tag("ServiceMediator").d("Loading $serviceId ($loadType)")
    return try {
      // The network load method takes an optional after=<user.id>
      // parameter. For every page after the first, pass the last user
      // ID to let it continue from where it left off. For REFRESH,
      // pass null to load the first page.
      var pageOffset = 0
      val loadKey =
        when (loadType) {
          REFRESH -> {
            service.meta().firstPageKey?.toString().also {
              Timber.tag("ServiceMediator").d("Refreshing $serviceId with key $it")
            }
          }
          // In this example, you never need to prepend, since REFRESH
          // will always load the first page in the list. Immediately
          // return, reporting end of pagination.
          LoadType.PREPEND -> {
            return MediatorResult.Success(endOfPaginationReached = true)
          }
          LoadType.APPEND -> {
            pageOffset = state.lastItemOrNull()?.indexInResponse ?: 0
            Timber.tag("ServiceMediator")
              .d("Appending to $serviceId. Current page count: ${state.pages.size}")
            // Query DB for ServiceRemoteKey for the service.
            // ServiceRemoteKey is a wrapper object we use to keep track of page keys we
            // receive from the service to fetch the next or previous page.
            val remoteKey =
              catchUpDatabase.withTransaction { remoteKeyDao.remoteKeyByItem(serviceId) }

            // We must explicitly check if the page key is null when appending, since the
            // Reddit API informs the end of the list by returning null for page key, but
            // passing a null key to Reddit API will fetch the initial page.
            if (remoteKey.nextPageKey == null) {
              Timber.tag("ServiceMediator")
                .d("Appending $serviceId with null key. End of pagination")
              return MediatorResult.Success(endOfPaginationReached = true)
            }

            remoteKey.nextPageKey.also {
              Timber.tag("ServiceMediator").d("Appending $serviceId with key '$it'")
            }
          }
        }

      // Suspending network load via Retrofit. This doesn't need to be
      // wrapped in a withContext(Dispatcher.IO) { ... } block since
      // Retrofit's Coroutine CallAdapter dispatches on a worker
      // thread.
      Timber.tag("ServiceMediator").d("Fetching $serviceId with key '$loadKey'")
      // Need to wrap in IO due to
      // https://github.com/square/retrofit/issues/3363#issuecomment-1371767242
      val result =
        withContext(Dispatchers.IO) {
          val initialResult =
            service.fetch(
              DataRequest(
                pageKey = loadKey,
                pageOffset = pageOffset,
                limit =
                  when (loadType) {
                    REFRESH -> state.config.initialLoadSize
                    else -> state.config.pageSize
                  }
              )
            )
          val items = initialResult.items
          // Remap items with content types if they're not set.
          // Using RxJava's concatMapEager here because there's no alternative in Flow.
          initialResult.copy(
            items =
              Flowable.fromIterable(items)
                .concatMapEager { item ->
                  item.clickUrl?.let { clickUrl ->
                    if (item.contentType == null) {
                      return@concatMapEager Flowable.just(
                        item.copy(contentType = contentTypeChecker.contentType(clickUrl))
                      )
                    }
                  }
                  Flowable.just(item)
                }
                .toList()
                .blockingGet()
          )
        }

      Timber.tag("ServiceMediator").d("Updating DB $serviceId with key '$loadKey'")
      catchUpDatabase.withTransaction {
        if (loadType == REFRESH) {
          Timber.tag("ServiceMediator").d("Clearing DB $serviceId")
          serviceDao.deleteByService(serviceId)
          serviceDao.deleteOperationsByService(serviceId)
          remoteKeyDao.deleteByService(serviceId)
        }

        remoteKeyDao.insert(ServiceRemoteKey(serviceId, result.nextPageKey))
        serviceDao.insertAll(result.items)
        serviceDao.putOperation(
          OperationJournalEntry(System.currentTimeMillis(), serviceId, "insert")
        )
      }

      MediatorResult.Success(
        endOfPaginationReached = result.nextPageKey == null || result.items.isEmpty()
      )
    } catch (e: IOException) {
      MediatorResult.Error(e)
    } catch (e: HttpException) {
      MediatorResult.Error(e)
    }
  }

  override suspend fun initialize(): InitializeAction {
    val cacheTimeout = TimeUnit.MILLISECONDS.convert(1, TimeUnit.HOURS)
    val lastUpdate = catchUpDatabase.withTransaction { serviceDao.lastUpdated(serviceId) }
    return if (lastUpdate != null && (System.currentTimeMillis() - lastUpdate >= cacheTimeout)) {
      // Cached data is up-to-date, so there is no need to re-fetch
      // from the network.
      Timber.tag("ServiceMediator").d("Cached data is up-to-date for $serviceId")
      InitializeAction.SKIP_INITIAL_REFRESH
    } else {
      // Need to refresh cached data from network; returning
      // LAUNCH_INITIAL_REFRESH here will also block RemoteMediator's
      // APPEND and PREPEND from running until REFRESH succeeds.
      Timber.tag("ServiceMediator").d("Cached data is out-of-date for $serviceId")
      InitializeAction.LAUNCH_INITIAL_REFRESH
    }
  }
}
