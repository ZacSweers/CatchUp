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
import android.support.graphics.drawable.AnimatedVectorDrawableCompat
import android.support.v4.util.Pair
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.RxViewHolder
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import butterknife.BindView
import butterknife.OnClick
import butterknife.Unbinder
import com.google.auto.value.AutoValue
import com.jakewharton.rxbinding2.view.RxView
import com.uber.autodispose.kotlin.autoDisposeWith
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.sweers.catchup.R
import io.sweers.catchup.ui.InfiniteScrollListener
import io.sweers.catchup.ui.Scrollable
import io.sweers.catchup.util.Iterables
import io.sweers.catchup.util.Strings
import io.sweers.catchup.util.format
import jp.wasabeef.recyclerview.animators.FadeInUpAnimator
import org.threeten.bp.Instant
import retrofit2.HttpException
import timber.log.Timber
import java.io.IOException
import java.security.InvalidParameterException
import java.util.LinkedHashSet
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
  private val fromSaveInstanceState = false
  private var moreDataAvailable = true
  var dataLoading = false

  constructor() : super()

  constructor(args: Bundle) : super(args)

  override fun isDataLoading(): Boolean = dataLoading

  /**
   * View binding implementation to bind the given datum to the `holder`.
   *
   * @param item The datum to back the view with.
   * @param holder The item ViewHolder instance.
   */
  protected abstract fun bindItemView(item: T, holder: NewsItemViewHolder)

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
    //recyclerView.setItemAnimator(itemAnimator);
  }

  @OnClick(R.id.retry_button) internal fun onRetry() {
    errorView.visibility = GONE
    progress.visibility = VISIBLE
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

  override fun onSaveInstanceState(outState: Bundle) {
    // TODO Check when these are called in conductor, restore seems to be after attach.
    //outState.putInt("pageNumber", page);
    //outState.putBoolean("savedInstance", true);
    super.onSaveInstanceState(outState)
  }

  override fun onRestoreInstanceState(savedInstanceState: Bundle) {
    super.onRestoreInstanceState(savedInstanceState)
    //page = savedInstanceState.getInt("pageNumber");
    //fromSaveInstanceState = savedInstanceState.getBoolean("savedInstance", false);
  }

  protected fun setMoreDataAvailable(moreDataAvailable: Boolean) {
    this.moreDataAvailable = moreDataAvailable
  }

  private fun loadData(fromRefresh: Boolean = false) {
    if (fromRefresh) {
      moreDataAvailable = true
      page = 0
    }
    if (!moreDataAvailable) {
      return
    }
    val pageToRequest = page++
    dataLoading = true
    if (adapter.itemCount != 0) {
      recyclerView.post { adapter.dataStartedLoading() }
    }
    val timer = AtomicLong()
    getDataSingle(DataRequest.create(fromRefresh,
        fromSaveInstanceState && page != 0,
        pageToRequest)).observeOn(AndroidSchedulers.mainThread())
        .doOnEvent { _, _ ->
          swipeRefreshLayout.isEnabled = true
          swipeRefreshLayout.isRefreshing = false
        }
        .doOnSubscribe { timer.set(System.currentTimeMillis()) }
        .doFinally {
          Timber.d("Data load - %s - took: %dms",
              javaClass.simpleName,
              System.currentTimeMillis() - timer.get())
          dataLoading = false
          recyclerView.post { adapter.dataFinishedLoading() }
        }
        .autoDisposeWith(this)
        .subscribe({ data ->
          progress.visibility = GONE
          errorView.visibility = GONE
          swipeRefreshLayout.visibility = VISIBLE
          recyclerView.post {
            if (fromRefresh) {
              adapter.setData(data)
            } else {
              adapter.addData(data)
            }
          }
        }, { e ->
          val activity = activity
          if (pageToRequest == 0 && activity != null) {
            if (e is IOException) {
              progress.visibility = GONE
              errorTextView.text = "Network Problem"
              swipeRefreshLayout.visibility = GONE
              errorView.visibility = VISIBLE
              AnimatedVectorDrawableCompat.create(activity, R.drawable.avd_no_connection)?.run {
                errorImage.setImageDrawable(this)
                start()
              }
            } else if (e is HttpException) {
              // TODO Show some sort of API error response.
              progress.visibility = GONE
              errorTextView.text = "API Problem"
              swipeRefreshLayout.visibility = GONE
              errorView.visibility = VISIBLE
              AnimatedVectorDrawableCompat.create(activity, R.drawable.avd_no_connection)?.run {
                errorImage.setImageDrawable(this)
                start()
              }
            } else {
              // TODO Show some sort of generic response error
              progress.visibility = GONE
              swipeRefreshLayout.visibility = GONE
              errorTextView.text = "Unknown issue."
              errorView.visibility = VISIBLE
              AnimatedVectorDrawableCompat.create(activity, R.drawable.avd_no_connection)?.run {
                errorImage.setImageDrawable(this)
                start()
              }
              Timber.e(e, "Unknown issue.")
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

  private class Adapter<in T : HasStableId>(
      private val bindDelegate: (T, NewsItemViewHolder) -> Unit)
    : RecyclerView.Adapter<RecyclerView.ViewHolder>(), DataLoadingSubject.DataLoadingCallbacks {

    private val data = LinkedHashSet<T>()
    private var showLoadingMore = false

    init {
      setHasStableIds(true)
    }

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
        ServiceController.TYPE_ITEM -> return NewsItemViewHolder(
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
          bindDelegate(Iterables.get(data, position), holder as NewsItemViewHolder)
        } catch (e: Exception) {
          Timber.e(e, "Bind delegate failure!")
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

    fun addData(newData: List<T>) {
      val prevSize = data.size
      data.addAll(newData)
      notifyItemRangeInserted(prevSize, data.size - prevSize)
    }

    fun setData(newData: List<T>) {
      data.clear()
      data.addAll(newData)
      notifyDataSetChanged()
    }
  }

  @AutoValue
  abstract class DataRequest {

    abstract fun fromRefresh(): Boolean

    abstract fun multipage(): Boolean

    abstract fun page(): Int

    companion object {

      internal fun create(fromRefresh: Boolean, multipage: Boolean, page: Int): DataRequest {
        return AutoValue_BaseNewsController_DataRequest(fromRefresh, multipage, page)
      }
    }
  }

  class NewsItemViewHolder(itemView: View) : RxViewHolder(itemView) {

    @BindView(R.id.container) lateinit var container: View
    @BindView(R.id.title) lateinit var title: TextView
    @BindView(R.id.score) lateinit var score: TextView
    @BindView(R.id.score_divider) lateinit var scoreDivider: TextView
    @BindView(R.id.timestamp) lateinit var timestamp: TextView
    @BindView(R.id.author) lateinit var author: TextView
    @BindView(R.id.author_divider) lateinit var authorDivider: TextView
    @BindView(R.id.source) lateinit var source: TextView
    @BindView(R.id.comments) lateinit var comments: TextView
    @BindView(R.id.tag) lateinit var tag: TextView
    @BindView(R.id.tag_divider) lateinit var tagDivider: View
    private var unbinder: Unbinder? = null

    init {
      unbinder?.unbind()
      unbinder = `BaseNewsController$NewsItemViewHolder_ViewBinding`(this, itemView)
    }

    fun itemClicks(): Observable<Any> {
      return RxView.clicks(container)
    }

    fun itemLongClicks(): Observable<Any> {
      return RxView.longClicks(container)
    }

    fun itemCommentClicks(): Observable<Any> {
      return RxView.clicks(comments)
    }

    fun title(titleText: CharSequence?) {
      title.text = titleText
    }

    fun score(scoreValue: Pair<String, Int>?) {
      if (scoreValue == null) {
        score.visibility = GONE
        scoreDivider.visibility = GONE
      } else {
        scoreDivider.visibility = VISIBLE
        score.visibility = VISIBLE
        score.text = String.format("%s %s",
            scoreValue.first,
            scoreValue.second.toLong().format())
      }
    }

    fun tag(text: String?) {
      if (text == null) {
        tag.visibility = GONE
        tagDivider.visibility = GONE
      } else {
        tag.visibility = VISIBLE
        tagDivider.visibility = VISIBLE
        tag.text = Strings.capitalize(text)
      }
    }

    fun timestamp(instant: Instant) {
      timestamp(instant.toEpochMilli())
    }

    private fun timestamp(date: Long) {
      timestamp.text = DateUtils.getRelativeTimeSpanString(date,
          System.currentTimeMillis(),
          0L,
          DateUtils.FORMAT_ABBREV_ALL)
    }

    fun author(authorText: CharSequence?) {
      if (authorText == null) {
        author.visibility = GONE
        authorDivider.visibility = GONE
      } else {
        authorDivider.visibility = VISIBLE
        author.visibility = VISIBLE
        author.text = authorText
      }
    }

    fun source(sourceText: CharSequence?) {
      if (sourceText == null) {
        source.visibility = GONE
        authorDivider.visibility = GONE
      } else {
        if (author.visibility == VISIBLE) {
          authorDivider.visibility = VISIBLE
        }
        source.visibility = VISIBLE
        source.text = sourceText
      }
    }

    fun comments(commentsCount: Int) {
      comments.text = commentsCount.toLong().format()
    }

    fun hideComments() {
      comments.visibility = GONE
    }
  }
}
