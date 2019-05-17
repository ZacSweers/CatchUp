/*
 * Copyright (C) 2019. Zac Sweers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
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
package io.sweers.catchup.ui

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.sweers.catchup.ui.base.DataLoadingSubject

/**
 * A scroll listener for RecyclerView to load more items as you approach the end.
 *
 * Adapted from [here](https://gist.github.com/ssinss/e06f12ef66c51252563e)
 */
abstract class InfiniteScrollListener(
  private val layoutManager: LinearLayoutManager,
  private val dataLoading: DataLoadingSubject
) : RecyclerView.OnScrollListener() {

  override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
    // bail out if scrolling upward or already loading data
    if (dy < 0 || dataLoading.isDataLoading()) {
      return
    }

    val visibleItemCount = recyclerView.childCount
    val totalItemCount = layoutManager.itemCount
    val firstVisibleItem = layoutManager.findFirstVisibleItemPosition()

    if (totalItemCount - visibleItemCount <= firstVisibleItem + VISIBLE_THRESHOLD) {
      onLoadMore()
    }
  }

  abstract fun onLoadMore()

  companion object {

    // The minimum number of items remaining before we should loading more.
    private const val VISIBLE_THRESHOLD = 5
  }
}
