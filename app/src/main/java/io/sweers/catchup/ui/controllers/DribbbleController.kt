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

package io.sweers.catchup.ui.controllers

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.graphics.ColorMatrixColorFilter
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.TransitionDrawable
import android.os.Build
import android.os.Bundle
import android.support.annotation.ArrayRes
import android.support.annotation.ColorInt
import android.support.graphics.drawable.AnimatedVectorDrawableCompat
import android.support.v4.view.animation.FastOutSlowInInterpolator
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.RxViewHolder
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import butterknife.BindView
import butterknife.OnClick
import butterknife.Unbinder
import com.bluelinelabs.conductor.Controller
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.jakewharton.rxbinding2.view.RxView
import com.squareup.moshi.Moshi
import com.uber.autodispose.kotlin.autoDisposeWith
import dagger.Binds
import dagger.Lazy
import dagger.Provides
import dagger.Subcomponent
import dagger.android.AndroidInjector
import dagger.multibindings.IntoMap
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.sweers.catchup.BuildConfig
import io.sweers.catchup.R
import io.sweers.catchup.data.AuthInterceptor
import io.sweers.catchup.data.ISO8601InstantAdapter
import io.sweers.catchup.data.LinkManager
import io.sweers.catchup.data.LinkManager.UrlMeta
import io.sweers.catchup.data.dribbble.DribbbleService
import io.sweers.catchup.data.dribbble.model.Shot
import io.sweers.catchup.injection.ControllerKey
import io.sweers.catchup.ui.InfiniteScrollListener
import io.sweers.catchup.ui.Scrollable
import io.sweers.catchup.ui.base.DataLoadingSubject
import io.sweers.catchup.ui.base.ServiceController
import io.sweers.catchup.ui.widget.BadgedFourThreeImageView
import io.sweers.catchup.util.Iterables
import io.sweers.catchup.util.ObservableColorMatrix
import io.sweers.catchup.util.UiUtil
import io.sweers.catchup.util.collect.cast
import io.sweers.catchup.util.glide.DribbbleTarget
import okhttp3.OkHttpClient
import org.threeten.bp.Instant
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import timber.log.Timber
import java.io.IOException
import java.util.ArrayList
import javax.inject.Inject
import javax.inject.Qualifier

class DribbbleController : ServiceController, SwipeRefreshLayout.OnRefreshListener, Scrollable, DataLoadingSubject {
  @Inject lateinit var service: DribbbleService
  @Inject lateinit var linkManager: LinkManager
  @BindView(R.id.error_container) lateinit var errorView: View
  @BindView(R.id.error_image) lateinit var errorImage: ImageView
  @BindView(R.id.error_message) lateinit var errorTextView: TextView
  @BindView(R.id.list) lateinit var recyclerView: RecyclerView
  @BindView(R.id.progress) lateinit var progress: ProgressBar
  @BindView(R.id.refresh) lateinit var swipeRefreshLayout: SwipeRefreshLayout
  private lateinit var adapter: Adapter
  private var page = 1
  private var isDataLoading = false

  constructor() : super()

  constructor(args: Bundle) : super(args)

  override fun onThemeContext(context: Context): Context {
    return ContextThemeWrapper(context, R.style.CatchUp_Dribbble)
  }

  override fun inflateView(inflater: LayoutInflater, container: ViewGroup): View {
    return inflater.inflate(R.layout.controller_basic_news, container, false)
  }

  override fun bind(view: View): Unbinder {
    return DribbbleController_ViewBinding(this, view)
  }

  override fun onViewBound(view: View) {
    super.onViewBound(view)

    swipeRefreshLayout.setColorSchemeColors(serviceThemeColor)

    val layoutManager = GridLayoutManager(activity, 2)
    recyclerView.layoutManager = layoutManager
    adapter = Adapter(view.context,
        { shot, viewHolder ->
          RxView.clicks(viewHolder.itemView)
              .compose<UrlMeta>(transformUrlToMeta<Any>(shot.htmlUrl()))
              .flatMapCompletable(linkManager)
              .autoDisposeWith(viewHolder)
              .subscribe()
        })
    recyclerView.adapter = adapter
    recyclerView.addOnScrollListener(
        object : InfiniteScrollListener(layoutManager, this@DribbbleController) {
          override fun onLoadMore() {
            loadData()
          }
        })
    layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
      override fun getSpanSize(position: Int): Int {
        return adapter.getItemColumnSpan(position)
      }
    }
    swipeRefreshLayout.setOnRefreshListener(this)
  }

  override fun onAttach(view: View) {
    super.onAttach(view)
    swipeRefreshLayout.isEnabled = false
    loadData()
  }

  override fun isDataLoading(): Boolean {
    return isDataLoading
  }

  private fun loadData() {
    val currentPage = page++
    isDataLoading = true
    if (adapter.itemCount != 0) {
      adapter.dataStartedLoading()
    }
    service.getPopular(currentPage, 50)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnEvent { _, _ ->
          swipeRefreshLayout.isEnabled = true
          swipeRefreshLayout.isRefreshing = false
        }
        .doFinally {
          isDataLoading = false
          adapter.dataFinishedLoading()
        }
        .autoDisposeWith(this)
        .subscribe({ shots ->
          progress.visibility = GONE
          errorView.visibility = GONE
          swipeRefreshLayout.visibility = VISIBLE
          adapter.addShots(shots)
        }, { e ->
          val activity = activity
          if (activity != null) {
            if (e is IOException) {
              errorTextView.text = "Network Problem"
              progress.visibility = GONE
              swipeRefreshLayout.visibility = GONE
              errorView.visibility = VISIBLE
              AnimatedVectorDrawableCompat.create(activity, R.drawable.avd_no_connection)?.run {
                errorImage.setImageDrawable(this)
                start()
              }
            } else if (e is HttpException) {
              // TODO Show some sort of API error response.
              errorTextView.text = "API Problem"
              progress.visibility = GONE
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
              errorTextView.text = "Unknown Issue"
              errorView.visibility = VISIBLE
              AnimatedVectorDrawableCompat.create(activity, R.drawable.avd_no_connection)?.run {
                errorImage.setImageDrawable(this)
                start()
              }
            }
            Timber.e(e, "Update failed!")
          }
        })
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

  override fun onRefresh() {
    loadData()
  }

  override fun onRequestScrollToTop() {
    if (adapter.itemCount > 50) {
      recyclerView.scrollToPosition(0)
    } else {
      recyclerView.smoothScrollToPosition(0)
    }
  }

  @Subcomponent
  interface Component : AndroidInjector<DribbbleController> {

    @Subcomponent.Builder
    abstract class Builder : AndroidInjector.Builder<DribbbleController>()
  }

  private class Adapter(context: Context,
      private val bindDelegate: (Shot, Adapter.DribbbleShotHolder) -> Unit)
    : RecyclerView.Adapter<RecyclerView.ViewHolder>(), DataLoadingSubject.DataLoadingCallbacks {

    private val shots = ArrayList<Shot>()
    private val shotLoadingPlaceholders: Array<ColorDrawable>
    private var showLoadingMore = false

    init {
      setHasStableIds(true)
      @ArrayRes val loadingColorArrayId: Int
      if (UiUtil.isInNightMode(context)) {
        loadingColorArrayId = R.array.loading_placeholders_dark
      } else {
        loadingColorArrayId = R.array.loading_placeholders_light
      }
      shotLoadingPlaceholders = context.resources.getIntArray(loadingColorArrayId)
          .iterator()
          .asSequence()
          .map(::ColorDrawable)
          .toList()
          .toTypedArray()
    }

    fun addShots(newShots: List<Shot>) {
      val prevSize = shots.size
      shots.addAll(newShots)
      notifyItemRangeInserted(prevSize, shots.size - prevSize)
    }

    override fun getItemId(position: Int): Long {
      if (getItemViewType(position) == ServiceController.TYPE_LOADING_MORE) {
        return RecyclerView.NO_ID
      }
      return Iterables.get(shots, position)
          .stableId()
    }

    @SuppressLint("ClickableViewAccessibility")
    @TargetApi(Build.VERSION_CODES.M)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder? {
      val layoutInflater = LayoutInflater.from(parent.context)
      when (viewType) {
        ServiceController.TYPE_ITEM -> {
          val holder = DribbbleShotHolder(LayoutInflater.from(parent.context)
              .inflate(R.layout.dribbble_shot_item, parent, false))
          holder.image.setBadgeColor(INITIAL_GIF_BADGE_COLOR)
          holder.image.foreground = UiUtil.createColorSelector(0x40808080,
              null)
          // play animated GIFs whilst touched
          holder.image.setOnTouchListener { _, event ->
            // check if it's an event we care about, else bail fast
            val action = event.action
            if (!(action == MotionEvent.ACTION_DOWN
                || action == MotionEvent.ACTION_UP
                || action == MotionEvent.ACTION_CANCEL)) {
              return@setOnTouchListener false
            }

            // get the image and check if it's an animated GIF
            val drawable = holder.image.drawable ?: return@setOnTouchListener false
            var gif: GifDrawable = when (drawable) {
              is GifDrawable -> drawable
              is TransitionDrawable -> (0..drawable.numberOfLayers - 1).asSequence()
                  .map { i -> drawable.getDrawable(i) }
                  .filter { it is GifDrawable }
                  .cast<GifDrawable>()
                  .firstOrNull()
              else -> null
            } ?: return@setOnTouchListener false
            // GIF found, start/stop it on press/lift
            when (action) {
              MotionEvent.ACTION_DOWN -> gif.start()
              MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> gif.stop()
            }
            false
          }
          return holder
        }
        ServiceController.TYPE_LOADING_MORE -> return ServiceController.LoadingMoreHolder(
            layoutInflater.inflate(R.layout.infinite_loading, parent, false))
      }
      return null
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
      when (getItemViewType(position)) {
        ServiceController.TYPE_ITEM -> {
          (holder as DribbbleShotHolder).bindView(shots[position])
          bindDelegate(shots[position], holder)
        }
        ServiceController.TYPE_LOADING_MORE -> (holder as ServiceController.LoadingMoreHolder).progress.visibility = if (position > 0) View.VISIBLE else View.INVISIBLE
      }
    }

    @SuppressLint("NewApi") override fun onViewRecycled(holder: RecyclerView.ViewHolder?) {
      if (holder is DribbbleShotHolder) {
        // reset the badge & ripple which are dynamically determined
        holder.image.setBadgeColor(INITIAL_GIF_BADGE_COLOR)
        holder.image.showBadge(false)
        holder.image.foreground = UiUtil.createColorSelector(0x40808080, null)
      }
    }

    override fun getItemCount(): Int {
      return dataItemCount + if (showLoadingMore) 1 else 0
    }

    val dataItemCount: Int
      get() = shots.size

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

    fun getItemColumnSpan(position: Int): Int {
      when (getItemViewType(position)) {
        ServiceController.TYPE_LOADING_MORE -> return 2
        else -> return 1
      }
    }

    internal inner class DribbbleShotHolder(itemView: View) : RxViewHolder(itemView) {

      internal val image: BadgedFourThreeImageView = itemView as BadgedFourThreeImageView

      fun bindView(shot: Shot) {
        val imageSize = shot.images()
            .bestSize()
        Glide.with(itemView.context)
            .load(shot.images()
                .best())
            .apply(RequestOptions().placeholder(
                shotLoadingPlaceholders[adapterPosition % shotLoadingPlaceholders.size])
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .fitCenter()
                .override(imageSize[0], imageSize[1]))
            .transition(DrawableTransitionOptions.withCrossFade())
            .listener(object : RequestListener<Drawable> {
              override fun onResourceReady(resource: Drawable,
                  model: Any,
                  target: Target<Drawable>,
                  dataSource: DataSource,
                  isFirstResource: Boolean): Boolean {
                if (!shot.hasFadedIn) {
                  image.setHasTransientState(true)
                  val cm = ObservableColorMatrix()
                  val saturation = ObjectAnimator.ofFloat(cm, ObservableColorMatrix.SATURATION, 0f,
                      1f)
                  saturation.addUpdateListener { _ ->
                    // just animating the color matrix does not invalidate the
                    // drawable so need this update listener.  Also have to create a
                    // new CMCF as the matrix is immutable :(
                    image.colorFilter = ColorMatrixColorFilter(cm)
                  }
                  saturation.duration = 2000L
                  saturation.interpolator = FastOutSlowInInterpolator()
                  saturation.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                      image.clearColorFilter()
                      image.setHasTransientState(false)
                    }
                  })
                  saturation.start()
                  shot.hasFadedIn = true
                }
                return false
              }

              override fun onLoadFailed(e: GlideException?,
                  model: Any,
                  target: Target<Drawable>,
                  isFirstResource: Boolean): Boolean {
                return false
              }
            })
            .into(DribbbleTarget(image, false))
        // need both placeholder & background to prevent seeing through shot as it fades in
        image.background = shotLoadingPlaceholders[adapterPosition % shotLoadingPlaceholders.size]
        image.showBadge(shot.animated())
      }
    }
  }

  @dagger.Module(subcomponents = arrayOf(Component::class))
  abstract class Module {

    @Qualifier
    private annotation class InternalApi

    @Binds
    @IntoMap
    @ControllerKey(DribbbleController::class)
    internal abstract fun bindDribbbleControllerInjectorFactory(
        builder: Component.Builder): AndroidInjector.Factory<out Controller>

    @dagger.Module
    companion object {

      @Provides @InternalApi @JvmStatic internal fun provideDribbbleOkHttpClient(
          client: OkHttpClient): OkHttpClient {
        return client.newBuilder()
            .addInterceptor(AuthInterceptor.create("Bearer",
                BuildConfig.DRIBBBLE_CLIENT_ACCESS_TOKEN))
            .build()
      }

      @Provides @InternalApi @JvmStatic internal fun provideDribbbleMoshi(moshi: Moshi): Moshi {
        return moshi.newBuilder()
            .add(Instant::class.java, ISO8601InstantAdapter())
            .build()
      }

      @Provides
      @JvmStatic
      internal fun provideDribbbleService(@InternalApi client: Lazy<OkHttpClient>,
          @InternalApi moshi: Moshi,
          rxJavaCallAdapterFactory: RxJava2CallAdapterFactory): DribbbleService {
        return Retrofit.Builder().baseUrl(DribbbleService.ENDPOINT)
            .callFactory { request ->
              client.get()
                  .newCall(request)
            }
            .addCallAdapterFactory(rxJavaCallAdapterFactory)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .validateEagerly(BuildConfig.DEBUG)
            .build()
            .create(DribbbleService::class.java)
      }
    }
  }

  companion object {

    @ColorInt private const val INITIAL_GIF_BADGE_COLOR = 0x40ffffff
  }
}
