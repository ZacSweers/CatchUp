/*
 * Copyright (c) 2018 Zac Sweers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.sweers.catchup.ui.fragments.service

import android.util.Log
import io.sweers.catchup.BuildConfig
import io.sweers.catchup.analytics.trace
import io.sweers.catchup.data.ServiceDao
import io.sweers.catchup.data.ServicePage
import io.sweers.catchup.data.getFirstServicePage
import io.sweers.catchup.data.getItemByIds
import io.sweers.catchup.data.getServicePage
import io.sweers.catchup.data.putItems
import io.sweers.catchup.data.putPage
import io.sweers.catchup.service.api.BindableCatchUpItemViewHolder
import io.sweers.catchup.service.api.CatchUpItem
import io.sweers.catchup.service.api.DataRequest
import io.sweers.catchup.service.api.DataResult
import io.sweers.catchup.service.api.Service
import io.sweers.catchup.service.api.ServiceException
import io.sweers.catchup.util.w
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.reduce
import org.threeten.bp.Instant
import org.threeten.bp.temporal.ChronoUnit
import retrofit2.HttpException
import java.io.IOException

class StorageBackedService(
    private val dao: ServiceDao,
    private val delegate: Service) : Service {

  private var currentSessionId: Long = -1

  override fun rootService() = delegate

  override fun meta() = delegate.meta()

  @ObsoleteCoroutinesApi
  @ExperimentalCoroutinesApi
  override suspend fun fetchPage(request: DataRequest): DataResult {
    Log.d("COROUTINES", "fetchPage for ${meta().id} $request")
    if (request.multiPage) {
      Log.d("COROUTINES", "--multipage")
      // Backfill pages

      // If service has deterministic numeric paging, we can use range + concatMapEager to fast track it
//      if (meta().pagesAreNumeric) {
//        return IntRange(0, request.pageId.toInt())
//            .asSequence()
//            .concatMapEager { getPage(it.toString(), allowNetworkFallback = false) }
//            .reduce { prev, result ->
//              DataResult(prev.data + result.data, result.nextPageToken, wasFresh = false)
//            }
//      }

      // Strategy is a bit interesting here. We start with a synthesized initial "result"
      // in a Channel. We start with this in the stream, and then from there flatmap the next
      // page for every token. If the token is not null, we send it back to the stateHandler to fetch
      // the next page. If that page is our final page or it was null, complete the state handler.
      //
      // Basically a Channel way of generating an indefinite sequence. At the end, we reduce all the
      // results, continuously appending new data and keeping the last seen page token. The result
      // is the last emission has all the data we want plus the last "next page" token.
      val state = BroadcastChannel<DataResult>(1)
      state.send(DataResult(emptyList(), meta().firstPageKey))
      val finalResult = state.openSubscription()
          .reduce { prev, result ->
            // Complete if there's no more pages or we've hit the target page
            if (prev.nextPageToken != null && prev.nextPageToken != request.pageId) {
              // Always request the next page if it's not null. If it's the last page, we'll complete after this
              val newState = getPage(prev.nextPageToken!!, allowNetworkFallback = false)
              state.send(newState)
            } else {
              state.close()
            }

            DataResult(data = prev.data + result.data,
                nextPageToken = result.nextPageToken,
                wasFresh = false)
          }
      Log.d("COROUTINES", "--final result ${finalResult.data.size}")

      return if (finalResult.data.isEmpty()) {
        Log.d("COROUTINES", "--final result was empty")
        // Ultimately fall back to just trying to request the first page
        getPage(page = meta().firstPageKey,
            isRefresh = false,
            allowNetworkFallback = true).also {
          Log.d("COROUTINES", "--final result was empty, now network fallback ${it.data.size}")
        }
      } else {
        finalResult
      }
    } else {
      return getPage(request.pageId, request.fromRefresh).also {
        Log.d("COROUTINES", "--not multipage, regular fetch was ${it.data.size}")
      }
    }
  }

  private suspend fun getPage(page: String,
      isRefresh: Boolean = false,
      allowNetworkFallback: Boolean = true): DataResult {
    Log.d("COROUTINES", "--getPage ${meta().id} $page isRefresh=$isRefresh allowNetworkFallback=$allowNetworkFallback")
    if (!isRefresh) {
      // Try from local first
      // If no prev session ID, grab the latest page
      // If we do have a prev session ID, we want those pages
      val useLatest = currentSessionId == -1L
      if (BuildConfig.DEBUG && useLatest && page != meta().firstPageKey) {
        // This shouldn't happen. If we have no session, we should be fetching the first page
        w(IllegalStateException(
            "Fetching first local ($page) but not first page! Received $page for ${meta().id}")) {
          "invalid store state currentSessionId=$currentSessionId useLatest=$useLatest"
        }
      }
      Log.d("COROUTINES", "--getPage ${meta().id} $page uselatest=$useLatest")
      Log.d("COROUTINES", "--getPage ${meta().id} $page fetching from local")
      val localAttempt = fetchPageFromLocal(page, useLatest)
      if (localAttempt != null) return localAttempt
      Log.d("COROUTINES", "--getPage ${meta().id} $page first local attempt failed")
      if (!useLatest && page == meta().firstPageKey) {
        // If we were trying to a current session but failed, fall back to first local page
        Log.d("COROUTINES", "--getPage ${meta().id} $page falling back to fetchLocal with latest")
        val useLatestAttempt = fetchPageFromLocal(page, true)
        if (useLatestAttempt != null) return useLatestAttempt
      } else {
        Log.d("COROUTINES", "--getPage ${meta().id} $page skipping fallback to first local page")
      }
      if (allowNetworkFallback) {
        Log.d("COROUTINES", "--getPage ${meta().id} $page falling back to network with latest")
        // Nothing local, fall to network
        return fetchPageFromNetwork(page, isRefresh)
      } else {
        Log.d("COROUTINES", "--getPage ${meta().id} $page skipping network fallback")
      }
      throw ServiceException("No available data! ${meta().id} $page uselatest is $useLatest $page $isRefresh $allowNetworkFallback")
//      return fetchPageFromLocal(page, useLatest) ?: runIf(!useLatest && page == meta().firstPageKey) {
//        // If we were trying to a current session but failed, fall back to first local page
//        Log.d("COROUTINES", "--getPage ${meta().id} $page falling back to fetchLocal with latest")
//        fetchPageFromLocal(page, true)
//      } ?: runIf(allowNetworkFallback) {
//        Log.d("COROUTINES", "--getPage ${meta().id} $page falling back to network with latest")
//        // Nothing local, fall to network
//        fetchPageFromNetwork(page, isRefresh)
//      } ?: run {
//        throw ServiceException("No available data! ${meta().id} $page uselatest is $useLatest $page $isRefresh $allowNetworkFallback")
//      }
    } else {
      return fetchPageFromNetwork(page, true)
    }
  }

  private suspend fun fetchPageFromLocal(pageId: String, useLatest: Boolean): DataResult? {
    Log.d("COROUTINES", "--fetchPageFromLocal ${meta().id} $pageId uselatest=$useLatest")
    trace("Local data load - ${delegate.meta().id}") {
      val now = Instant.now()
      val page = with(dao) {
        if (pageId == meta().firstPageKey && useLatest) {
          Log.d("COROUTINES", "--getFirstServicePage ${meta().id} $pageId")
          getFirstServicePage(meta().id, page = pageId)
        } else {
          Log.d("COROUTINES", "--getServicePage ${meta().id} $pageId")
          getServicePage(type = meta().id, page = pageId, sessionId = currentSessionId)
        }
      }
      if (page == null || page.expiration.isAfter(now)) {
        Log.d("COROUTINES", "--fetchPageFromLocal ${meta().id} page=$page")
        return null
      }
      return page.let { servicePage ->
        if (pageId == meta().firstPageKey) {
          currentSessionId = servicePage.sessionId
          Log.d("COROUTINES", "--fetchPageFromLocal ${meta().id} currentSessionId=$currentSessionId")
        }
        // We want to preserve the ordering that we stored before, map ID -> index
        val idToIndex = servicePage.items.withIndex()
            .associateBy(keySelector = { (_, value) -> value }) { (index, _) -> index }
        Log.d("COROUTINES", "--fetchPageFromLocal ${meta().id} page=$page getItemByIds")
        dao.getItemByIds(servicePage.items.toTypedArray())
            ?.sortedBy { idToIndex[it.stableId()] }
            ?.let { DataResult(it, servicePage.nextPageToken, wasFresh = false) }
      }
    }
  }

  private suspend fun fetchPageFromNetwork(pageId: String, isRefresh: Boolean): DataResult {
    Log.d("COROUTINES", "--fetchPageFromNetwork ${meta().id} pageId=$pageId isRefresh=$isRefresh")
    return trace("Network data load - ${delegate.meta().id}") {
      try {
        delegate.fetchPage(DataRequest(true, false, pageId))
            .let { result ->
              Log.d("COROUTINES", "--fetchPageFromNetwork ${meta().id} result is ${result.data.size}")
              val calculatedExpiration = Instant.now()
                  .plus(2, ChronoUnit.HOURS) // TODO preference this
              if (currentSessionId == -1L || isRefresh) {
                currentSessionId = calculatedExpiration.toEpochMilli()
                Log.d("COROUTINES", "--fetchPageFromNetwork ${meta().id} currentSessionId=$currentSessionId")
              }
              trace("Network data store - ${delegate.meta().id}") {
                Log.d("COROUTINES", "--fetchPageFromNetwork ${meta().id} putting page and items")
                dao.putPage(
                    ServicePage(
                        id = "${meta().id}$pageId",
                        type = meta().id,
                        page = pageId,
                        items = result.data.map { it.stableId() },
                        expiration = calculatedExpiration,
                        sessionId = if (pageId == meta().firstPageKey && isRefresh) {
                          calculatedExpiration.toEpochMilli()
                        } else {
                          if (currentSessionId == -1L) {
                            throw IllegalStateException("wat")
                          }
                          currentSessionId
                        },
                        nextPageToken = result.nextPageToken
                    ))

                dao.putItems(*result.data.toTypedArray())
                Log.d("COROUTINES", "--fetchPageFromNetwork ${meta().id} putting page and items done")
              }

              return@let result
            }
      } catch (throwable: Throwable) {
        Log.d("COROUTINES", "--fetchPageFromNetwork ${meta().id} error $throwable")
        // At least *try* to gracefully handle it
        if (throwable is HttpException) {
          throw throwable
        } else if (pageId == meta().firstPageKey && !isRefresh && throwable is IOException) {
          Log.d("COROUTINES", "--fetchPageFromNetwork ${meta().id} error retrying from local")
          fetchPageFromLocal(pageId, true) ?: throw throwable
        } else {
          throw throwable
        }
      }
    }
  }

  override fun bindItemView(item: CatchUpItem, holder: BindableCatchUpItemViewHolder) {
    delegate.bindItemView(item, holder)
  }

  override fun linkHandler() = delegate.linkHandler()
}
