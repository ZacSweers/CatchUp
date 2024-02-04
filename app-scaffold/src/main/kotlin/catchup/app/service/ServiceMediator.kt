package catchup.app.service

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.LoadType.REFRESH
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import catchup.app.data.lastUpdated
import catchup.di.FakeMode
import catchup.service.api.CatchUpItem
import catchup.service.api.DataRequest
import catchup.service.api.DataResult
import catchup.service.api.Service
import catchup.service.api.VisualService
import catchup.service.api.toCatchUpDbItem
import catchup.service.db.CatchUpDatabase
import catchup.service.db.CatchUpDbItem
import com.apollographql.apollo3.exception.ApolloException
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import java.io.IOException
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import retrofit2.HttpException
import timber.log.Timber

@OptIn(ExperimentalPagingApi::class)
class ServiceMediator
@AssistedInject
constructor(
  @Assisted private val service: Service,
  @Assisted private val catchUpDatabase: CatchUpDatabase,
  @FakeMode private val isFakeMode: Boolean,
  private val contentTypeChecker: ContentTypeChecker,
  private val clock: Clock,
) : RemoteMediator<Int, CatchUpDbItem>() {

  @AssistedFactory
  fun interface Factory {
    fun create(service: Service, catchUpDatabase: CatchUpDatabase): ServiceMediator
  }

  private val serviceIdKey: String = service.meta().id

  override suspend fun load(
    loadType: LoadType,
    state: PagingState<Int, CatchUpDbItem>,
  ): MediatorResult {
    Timber.tag("ServiceMediator").d("Loading $serviceIdKey ($loadType)")
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
              Timber.tag("ServiceMediator").d("Refreshing $serviceIdKey with key $it")
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
              .d("Appending to $serviceIdKey. Current page count: ${state.pages.size}")
            // Query DB for ServiceRemoteKey for the service.
            // ServiceRemoteKey is a wrapper object we use to keep track of page keys we
            // receive from the service to fetch the next or previous page.
            val remoteKey =
              withContext(Dispatchers.IO) {
                catchUpDatabase.transactionWithResult {
                  catchUpDatabase.serviceQueries.remoteKeyByItem(serviceIdKey).executeAsOne()
                }
              }

            // We must explicitly check if the page key is null when appending, since the
            // Reddit API informs the end of the list by returning null for page key, but
            // passing a null key to Reddit API will fetch the initial page.
            if (remoteKey.nextPageKey == null) {
              Timber.tag("ServiceMediator")
                .d("Appending $serviceIdKey with null key. End of pagination")
              return MediatorResult.Success(endOfPaginationReached = true)
            }

            remoteKey.nextPageKey.also {
              Timber.tag("ServiceMediator").d("Appending $serviceIdKey with key '$it'")
            }
          }
        }

      // Suspending network load via Retrofit. This doesn't need to be
      // wrapped in a withContext(Dispatcher.IO) { ... } block since
      // Retrofit's Coroutine CallAdapter dispatches on a worker
      // thread.
      Timber.tag("ServiceMediator").d("Fetching $serviceIdKey with key '$loadKey'")
      val request =
        DataRequest(
          pageKey = loadKey,
          pageOffset = pageOffset,
          limit =
            when (loadType) {
              REFRESH -> state.config.initialLoadSize
              else -> state.config.pageSize
            },
          useFakeData = isFakeMode,
        )

      // Need to wrap in IO due to
      // https://github.com/square/retrofit/issues/3363#issuecomment-1371767242
      val result =
        if (request.useFakeData) {
          if (service.supportsFakeData) {
            withContext(Dispatchers.IO) { service.fetch(request) }
          } else {
            DataResult(
              items = CatchUpItem.fakeItems(request.limit, serviceIdKey, service is VisualService),
              nextPageKey = null,
            )
          }
        } else {
          withContext(Dispatchers.IO) {
            val initialResult = service.fetch(request)
            val items = initialResult.items
            // Remap items with content types if they're not set.
            // TODO concatMapEager?
            initialResult.copy(
              items =
                items
                  .asFlow()
                  .map { item ->
                    item.clickUrl?.let { clickUrl ->
                      if (item.contentType == null) {
                        return@map item.copy(contentType = contentTypeChecker.contentType(clickUrl))
                      }
                    }
                    item
                  }
                  .toList()
            )
          }
        }

      Timber.tag("ServiceMediator").d("Updating DB $serviceIdKey with key '$loadKey'")
      withContext(Dispatchers.IO) {
        catchUpDatabase.transaction {
          if (loadType == REFRESH) {
            Timber.tag("ServiceMediator").d("Clearing DB $serviceIdKey")
            catchUpDatabase.serviceQueries.deleteItemsByService(serviceIdKey)
            catchUpDatabase.serviceQueries.deleteOperationsByService(serviceIdKey)
            catchUpDatabase.serviceQueries.deleteRemoteKeyByService(serviceIdKey)
          }

          Timber.tag("ServiceMediator")
            .d("Inserting ${result.items.size} items into DB for '$serviceIdKey'")
          catchUpDatabase.serviceQueries.insertRemoteKey(serviceIdKey, result.nextPageKey)
          for (item in result.items) {
            catchUpDatabase.serviceQueries.insert(item.toCatchUpDbItem())
          }
          catchUpDatabase.serviceQueries.putOperation(
            clock.now().toEpochMilliseconds(),
            serviceIdKey,
            "insert",
          )
        }
      }

      MediatorResult.Success(
        endOfPaginationReached = result.nextPageKey == null || result.items.isEmpty()
      )
    } catch (e: IOException) {
      MediatorResult.Error(e)
    } catch (e: HttpException) {
      MediatorResult.Error(e)
    } catch (e: ApolloException) {
      // Annoying that this is a separate exception type.
      MediatorResult.Error(e)
    }
  }

  override suspend fun initialize(): InitializeAction {
    val cacheTimeout = 1.hours.inWholeMilliseconds
    val lastUpdate =
      withContext(Dispatchers.IO) {
        catchUpDatabase.transactionWithResult { catchUpDatabase.lastUpdated(serviceIdKey) }
      }
    return if (lastUpdate != null && (System.currentTimeMillis() - lastUpdate >= cacheTimeout)) {
      // Cached data is up-to-date, so there is no need to re-fetch
      // from the network.
      Timber.tag("ServiceMediator").d("Cached data is up-to-date for $serviceIdKey")
      InitializeAction.SKIP_INITIAL_REFRESH
    } else {
      // Need to refresh cached data from network; returning
      // LAUNCH_INITIAL_REFRESH here will also block RemoteMediator's
      // APPEND and PREPEND from running until REFRESH succeeds.
      Timber.tag("ServiceMediator").d("Cached data is out-of-date for $serviceIdKey")
      InitializeAction.LAUNCH_INITIAL_REFRESH
    }
  }
}
