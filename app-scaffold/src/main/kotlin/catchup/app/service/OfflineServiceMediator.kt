package catchup.app.service

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.paging.RemoteMediator.InitializeAction.SKIP_INITIAL_REFRESH

/** A [RemoteMediator] that does nothing. This is used in offline mode. */
@OptIn(ExperimentalPagingApi::class)
class OfflineRemoteMediator<Key : Any, Value : Any> : RemoteMediator<Key, Value>() {
  override suspend fun initialize() = SKIP_INITIAL_REFRESH

  override suspend fun load(loadType: LoadType, state: PagingState<Key, Value>): MediatorResult {
    return MediatorResult.Success(endOfPaginationReached = true)
  }
}
