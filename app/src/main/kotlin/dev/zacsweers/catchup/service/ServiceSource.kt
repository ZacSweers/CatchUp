package dev.zacsweers.catchup.service

import androidx.paging.PagingSource
import androidx.paging.PagingState
import io.sweers.catchup.service.api.CatchUpItem
import io.sweers.catchup.service.api.DataRequest
import io.sweers.catchup.service.api.Service
import kotlinx.coroutines.rx3.await

class ServiceSource(private val service: Service) : PagingSource<String, CatchUpItem>() {

  override fun getRefreshKey(state: PagingState<String, CatchUpItem>): String? {
    return null
  }

  override suspend fun load(params: LoadParams<String>): LoadResult<String, CatchUpItem> {
    return try {
      val paramKey = params.key
      val nextPage = paramKey ?: service.meta().firstPageKey
      val pageResult =
        service
          .fetchPage(
            DataRequest(fromRefresh = paramKey == null, multiPage = false, pageId = nextPage)
          )
          .await()
      val (data, nextPageToken, _) = pageResult

      LoadResult.Page(
        data = data,
        prevKey = if (paramKey == null) null else nextPage,
        nextKey = nextPageToken
      )
    } catch (e: Exception) {
      LoadResult.Error(e)
    }
  }
}
