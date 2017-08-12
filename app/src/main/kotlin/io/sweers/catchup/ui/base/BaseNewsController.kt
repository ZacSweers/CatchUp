/*
 * Copyright (c) 2017 Zac Sweers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.sweers.catchup.ui.base

import android.os.Bundle
import android.os.Parcelable
import android.support.graphics.drawable.AnimatedVectorDrawableCompat
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.util.DiffUtil
import android.support.v7.util.DiffUtil.DiffResult
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import butterknife.BindView
import butterknife.OnClick
import butterknife.Unbinder
import com.google.firebase.perf.FirebasePerformance
import com.uber.autodispose.kotlin.autoDisposeWith
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.sweers.catchup.R
import io.sweers.catchup.ui.InfiniteScrollListener
import io.sweers.catchup.ui.Scrollable
import io.sweers.catchup.ui.base.LoadResult.NewData
import io.sweers.catchup.ui.base.LoadResult.RefreshData
import io.sweers.catchup.util.Iterables
import io.sweers.catchup.util.d
import io.sweers.catchup.util.e
import io.sweers.catchup.util.isVisible
import io.sweers.catchup.util.makeGone
import io.sweers.catchup.util.makeVisible
import jp.wasabeef.recyclerview.animators.FadeInUpAnimator
import retrofit2.HttpException
import java.io.IOException
import java.security.InvalidParameterException
import java.util.concurrent.atomic.AtomicLong

abstract class BaseNewsController<T : HasStableId> : ServiceController,
    SwipeRefreshLayout.OnRefreshListener, Scrollable, DataLoadingSubject {

  @BindView(R.id.error_container) lateinit var errorView: View
  @BindView(R.id.error_message) lateinit var errorTextView: TextView
  @BindView(R.id.error_image) lateinit var errorImage: ImageView
  @BindView(R.id.list) lateinit var recyclerView: RecyclerView
  @BindView(R.id.progress) lateinit var progress: ProgressBar
  @BindView(R.id.refresh) lateinit var swipeRefreshLayout: SwipeRefreshLayout

  private lateinit var adapter: Adapter<T>
  private var page = 0
  private var isRestoring = false
  private var pageToRestoreTo = 0
  private var moreDataAvailable = true
  var dataLoading = false
  private var pendingRVState: Parcelable? = null

  constructor() : super()

  constructor(args: Bundle) : super(args)

  override fun isDataLoading(): Boolean = dataLoading

  /**
   * View binding implementation to bind the given datum to the `holder`.
   *
   * @param item The datum to back the view with.
   * @param holder The item ViewHolder instance.
   */
  protected abstract fun bindItemView(item: T, holder: CatchUpItemViewHolder)

  protected abstract fun getDataSingle(request: DataRequest): Single<List<T>>

  override fun inflateView(inflater: LayoutInflater, container: ViewGroup): View {
    return inflater.inflate(R.layout.controller_basic_news, container, false)
  }

  override fun bind(view: View): Unbinder {
    return BaseNewsController_ViewBinding(this, view)
  }

  override fun onViewBound(view: View) {
    super.onViewBound(view)

    swipeRefreshLayout.setColorSchemeColors(serviceThemeColor)

    val layoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
    recyclerView.layoutManager = layoutManager
    recyclerView.addOnScrollListener(
        object : InfiniteScrollListener(layoutManager, this@BaseNewsController) {
          override fun onLoadMore() {
            loadData()
          }
        })
    adapter = Adapter { t, holder -> this.bindItemView(t, holder) }
    recyclerView.adapter = adapter
    swipeRefreshLayout.setOnRefreshListener(this)

    val itemAnimator = FadeInUpAnimator(OvershootInterpolator(1f))
    itemAnimator.addDuration = 300
    itemAnimator.removeDuration = 300

    // This blows up adding item ranges >_>. Items start going to random places
//    recyclerView.itemAnimator = itemAnimator
  }

  @OnClick(R.id.retry_button) internal fun onRetry() {
    errorView.makeGone()
    progress.makeVisible()
    onRefresh()
  }

  @OnClick(R.id.error_image) internal fun onErrorClick(imageView: ImageView) {
    val avd = imageView.drawable as AnimatedVectorDrawableCompat
    avd.start()
  }

  override fun onAttach(view: View) {
    super.onAttach(view)
    swipeRefreshLayout.isEnabled = false
    loadData()
  }

  override fun onDetach(view: View) {
    page = 0
    moreDataAvailable = true
    super.onDetach(view)
  }

  override fun onSaveViewState(view: View, outState: Bundle) {
    outState.run {
      putInt("pageNumber", page)
      putParcelable("layoutManagerState", recyclerView.layoutManager.onSaveInstanceState())
    }
    super.onSaveViewState(view, outState)
  }

  override fun onRestoreViewState(view: View, savedViewState: Bundle) {
    super.onRestoreViewState(view, savedViewState)
    with(savedViewState) {
      pageToRestoreTo = getInt("pageNumber")
      pendingRVState = getParcelable("layoutManagerState")
      isRestoring = pendingRVState != null
    }
  }

  protected fun setMoreDataAvailable(moreDataAvailable: Boolean) {
    this.moreDataAvailable = moreDataAvailable
  }

  private fun loadData(fromRefresh: Boolean = false) {
    if (!recyclerView.isVisible()) {
      progress.makeVisible()
    }
    if (fromRefresh) {
      moreDataAvailable = true
      page = 0
    }
    if (!moreDataAvailable) {
      return
    }
    val pageToRequest = if (isRestoring) pageToRestoreTo.also { page = pageToRestoreTo } else page++
    dataLoading = true
    if (adapter.itemCount != 0) {
      recyclerView.post { adapter.dataStartedLoading() }
    }
    val trace = FirebasePerformance.getInstance().newTrace("Data load - ${javaClass.simpleName}")
    val timer = AtomicLong()
    getDataSingle(
        DataRequest(fromRefresh && !isRestoring,
            pageToRequest != 0,
            pageToRequest)
            .also {
              isRestoring = false
              pageToRestoreTo = 0
            })
        .map { newData ->
          if (fromRefresh) {
            LoadResult.RefreshData(newData,
                DiffUtil.calculateDiff(ItemUpdateCallback(adapter.getItems(), newData)))
          } else {
            LoadResult.NewData(newData)
          }
        }
        .observeOn(AndroidSchedulers.mainThread())
        .doOnEvent { _, _ ->
          swipeRefreshLayout.isEnabled = true
          swipeRefreshLayout.isRefreshing = false
        }
        .doOnSubscribe {
          trace.start()
          timer.set(System.currentTimeMillis())
        }
        .doFinally {
          trace.stop()
          d { "Data load - ${javaClass.simpleName} - took: ${System.currentTimeMillis() - timer.get()}ms" }
          dataLoading = false
          recyclerView.post { adapter.dataFinishedLoading() }
        }
        .autoDisposeWith(this)
        .subscribe({ loadResult ->
          progress.makeGone()
          errorView.makeGone()
          swipeRefreshLayout.makeVisible()
          recyclerView.post {
            when (loadResult) {
              is NewData -> {
                adapter.addData(loadResult.newData, true)
              }
              is RefreshData -> {
                with(loadResult) {
                  adapter.setData(data)
                  diffResult.dispatchUpdatesTo(adapter)
                }
              }
            }
            pendingRVState?.let {
              recyclerView.layoutManager.onRestoreInstanceState(it)
              pendingRVState = null
            }
          }
        }, { error ->
          val activity = activity
          if (pageToRequest == 0 && activity != null) {
            if (error is IOException) {
              progress.makeGone()
              errorTextView.text = "Connection Problem"
              swipeRefreshLayout.makeGone()
              errorView.makeVisible()
              AnimatedVectorDrawableCompat.create(activity, R.drawable.avd_no_connection)?.run {
                errorImage.setImageDrawable(this)
                start()
              }
            } else if (error is HttpException) {
              // TODO Show some sort of API error response.
              progress.makeGone()
              errorTextView.text = "API Problem"
              swipeRefreshLayout.makeGone()
              errorView.makeVisible()
              AnimatedVectorDrawableCompat.create(activity, R.drawable.avd_no_connection)?.run {
                errorImage.setImageDrawable(this)
                start()
              }
            } else {
              // TODO Show some sort of generic response error
              progress.makeGone()
              swipeRefreshLayout.makeGone()
              errorTextView.text = "Unknown issue."
              errorView.makeVisible()
              AnimatedVectorDrawableCompat.create(activity, R.drawable.avd_no_connection)?.run {
                errorImage.setImageDrawable(this)
                start()
              }
              e(error) { "Unknown issue." }
            }
          } else {
            page--
          }
        })
  }

  override fun onRefresh() {
    loadData(true)
  }

  override fun onRequestScrollToTop() {
    if (adapter.itemCount > 50) {
      recyclerView.scrollToPosition(0)
    } else {
      recyclerView.smoothScrollToPosition(0)
    }
  }

  private class Adapter<T : HasStableId>(
      private val bindDelegate: (T, CatchUpItemViewHolder) -> Unit)
    : RecyclerView.Adapter<RecyclerView.ViewHolder>(), DataLoadingSubject.DataLoadingCallbacks {

    private val data = mutableListOf<T>()
    private var showLoadingMore = false

    init {
      setHasStableIds(true)
    }

    fun getItems(): List<T> = data

    override fun getItemId(position: Int): Long {
      if (getItemViewType(position) == ServiceController.TYPE_LOADING_MORE) {
        return RecyclerView.NO_ID
      }
      return Iterables.get(data, position)
          .stableId()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
      val layoutInflater = LayoutInflater.from(parent.context)
      when (viewType) {
        ServiceController.TYPE_ITEM -> return CatchUpItemViewHolder(
            layoutInflater.inflate(R.layout.list_item_general,
                parent,
                false))
        ServiceController.TYPE_LOADING_MORE -> return ServiceController.LoadingMoreHolder(
            layoutInflater.inflate(R.layout.infinite_loading,
                parent,
                false))
      }
      throw InvalidParameterException("Unrecognized view type - " + viewType)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
      when (getItemViewType(position)) {
        ServiceController.TYPE_ITEM -> try {
          bindDelegate(Iterables.get(data, position), holder as CatchUpItemViewHolder)
        } catch (error: Exception) {
          e(error) { "Bind delegate failure!" }
        }

        ServiceController.TYPE_LOADING_MORE -> (holder as ServiceController.LoadingMoreHolder).progress.visibility = if (position > 0) View.VISIBLE else View.INVISIBLE
      }
    }

    override fun getItemCount(): Int {
      return dataItemCount + if (showLoadingMore) 1 else 0
    }

    val dataItemCount: Int
      get() = data.size

    private val loadingMoreItemPosition: Int
      get() = if (showLoadingMore) itemCount - 1 else RecyclerView.NO_POSITION

    override fun getItemViewType(position: Int): Int {
      if (position < dataItemCount && dataItemCount > 0) {
        return ServiceController.TYPE_ITEM
      }
      return ServiceController.TYPE_LOADING_MORE
    }

    override fun dataStartedLoading() {
      if (showLoadingMore) {
        return
      }
      showLoadingMore = true
      notifyItemInserted(loadingMoreItemPosition)
    }

    override fun dataFinishedLoading() {
      if (!showLoadingMore) {
        return
      }
      val loadingPos = loadingMoreItemPosition
      showLoadingMore = false
      notifyItemRemoved(loadingPos)
    }

    fun clearData(notify: Boolean = false) {
      data.clear()
      if (notify) {
        notifyDataSetChanged()
      }
    }

    fun addData(newData: List<T>, notify: Boolean = false) {
      val prevSize = data.size
      data.addAll(newData)
      if (notify) {
        notifyItemRangeInserted(prevSize, data.size - prevSize)
      }
    }

    fun setData(newData: List<T>, notify: Boolean = false) {
      data.clear()
      data.addAll(newData)
      if (notify) {
        notifyDataSetChanged()
      }
    }
  }

  data class DataRequest(val fromRefresh: Boolean,
      val multipage: Boolean,
      val page: Int)

}

private sealed class LoadResult<T> {
  data class RefreshData<T>(val data: List<T>, val diffResult: DiffResult) : LoadResult<T>()
  data class NewData<T>(val newData: List<T>) : LoadResult<T>()
}

private class ItemUpdateCallback<T : HasStableId>(
    private val oldItems: List<T>,
    private val newItems: List<T>
) : DiffUtil.Callback() {
  override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
    return oldItems[oldItemPosition].stableId() == newItems[newItemPosition].stableId()
  }

  override fun getOldListSize() = oldItems.size

  override fun getNewListSize() = newItems.size

  override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
    return oldItems[oldItemPosition] == newItems[newItemPosition]
  }
}
