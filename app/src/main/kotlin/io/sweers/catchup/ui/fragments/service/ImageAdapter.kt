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
package io.sweers.catchup.ui.fragments.service

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.graphics.ColorMatrixColorFilter
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.TransitionDrawable
import android.os.Build
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.OnClickListener
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import androidx.annotation.ArrayRes
import androidx.annotation.ColorInt
import androidx.core.animation.doOnEnd
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bumptech.glide.ListPreloader.PreloadModelProvider
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import io.sweers.catchup.GlideApp
import io.sweers.catchup.R
import io.sweers.catchup.R.layout
import io.sweers.catchup.service.api.BindableCatchUpItemViewHolder
import io.sweers.catchup.service.api.CatchUpItem
import io.sweers.catchup.service.api.TemporaryScopeHolder
import io.sweers.catchup.service.api.UrlMeta
import io.sweers.catchup.service.api.temporaryScope
import io.sweers.catchup.ui.base.DataLoadingSubject
import io.sweers.catchup.ui.widget.BadgedFourThreeImageView
import io.sweers.catchup.util.ObservableColorMatrix
import io.sweers.catchup.util.UiUtil
import io.sweers.catchup.util.glide.CatchUpTarget
import io.sweers.catchup.util.isInNightMode
import kotlinx.coroutines.channels.SendChannel

internal class ImageAdapter(
  private val context: Context,
  private val bindDelegate: (ImageItem, ImageHolder, clicksChannel: SendChannel<UrlMeta>) -> Unit
) :
  DisplayableItemAdapter<ImageItem, ViewHolder>(columnCount = 2),
    DataLoadingSubject.DataLoadingCallbacks,
    PreloadModelProvider<ImageItem> {

  companion object {
    const val PRELOAD_AHEAD_ITEMS = 6
    @ColorInt
    private const val INITIAL_GIF_BADGE_COLOR = 0x40ffffff
  }

  private val loadingPlaceholders: Array<ColorDrawable>
  private var showLoadingMore = false

  init {
    setHasStableIds(true)
    @ArrayRes val loadingColorArrayId = if (context.isInNightMode()) {
      R.array.loading_placeholders_dark
    } else {
      R.array.loading_placeholders_light
    }
    loadingPlaceholders = context.resources.getIntArray(loadingColorArrayId)
        .iterator()
        .asSequence()
        .map(::ColorDrawable)
        .toList()
        .toTypedArray()
  }

  override fun getPreloadItems(position: Int) =
      data.subList(position, minOf(data.size, position + 5))

  override fun getPreloadRequestBuilder(item: ImageItem): RequestBuilder<Drawable> {
    val (x, y) = item.imageInfo.bestSize ?: Pair(0, 0)
    return GlideApp.with(context)
        .asDrawable()
        .apply(RequestOptions()
            .diskCacheStrategy(DiskCacheStrategy.DATA)
            .fitCenter()
            .override(x, y))
        .load(item.realItem())
  }

  override fun getItemId(position: Int): Long {
    if (getItemViewType(position) == TYPE_LOADING_MORE) {
      return RecyclerView.NO_ID
    }
    return data[position].realItem().id
  }

  @TargetApi(Build.VERSION_CODES.M)
  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    val layoutInflater = LayoutInflater.from(parent.context)
    return when (viewType) {
      TYPE_ITEM -> {
        ImageHolder(LayoutInflater.from(parent.context)
            .inflate(layout.image_item, parent, false), loadingPlaceholders)
            .apply {
              image.setBadgeColor(
                  INITIAL_GIF_BADGE_COLOR)
              image.foreground = UiUtil.createColorSelector(0x40808080, null)
              // play animated GIFs whilst touched
              image.setOnTouchListener { _, event ->
                // check if it's an event we care about, else bail fast
                val action = event.action
                if (!(action == MotionEvent.ACTION_DOWN ||
                        action == MotionEvent.ACTION_UP ||
                        action == MotionEvent.ACTION_CANCEL)) {
                  return@setOnTouchListener false
                }

                // get the image and check if it's an animated GIF
                val gif: GifDrawable = when (val drawable = image.drawable
                    ?: return@setOnTouchListener false) {
                  is GifDrawable -> drawable
                  is TransitionDrawable -> (0 until drawable.numberOfLayers).asSequence()
                      .map(drawable::getDrawable)
                      .filterIsInstance<GifDrawable>()
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
            }
      }
      TYPE_LOADING_MORE -> LoadingMoreHolder(
          layoutInflater.inflate(layout.infinite_loading, parent, false))
      else -> TODO("Unknown type")
    }
  }

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    when (getItemViewType(position)) {
      TYPE_ITEM -> {
        val imageItem = data[position]
        val imageHolder = holder as ImageHolder
        if (imageHolder.backingImageItem?.stableId() == imageItem.stableId()) {
          // This is the same item, no need to reset. Possible from a refresh.
          return
        }
        // TODO This is kind of ugly but not sure what else to do. Holder can't be an inner class to avoid mem leaks
        imageHolder.backingImageItem = imageItem
        bindDelegate(imageItem, holder, clicksChannel())
            .also {
              holder.backingImageItem = null
            }
      }
      TYPE_LOADING_MORE -> (holder as LoadingMoreHolder).progress.visibility = if (position > 0) View.VISIBLE else View.INVISIBLE
    }
  }

  @SuppressLint("NewApi")
  override fun onViewRecycled(holder: ViewHolder) {
    super.onViewRecycled(holder)
    if (holder is ImageHolder) {
      // reset the badge & ripple which are dynamically determined
      GlideApp.with(holder.itemView).clear(holder.image)
      holder.image.setBadgeColor(
          INITIAL_GIF_BADGE_COLOR)
      holder.image.showBadge(false)
      holder.image.foreground = UiUtil.createColorSelector(0x40808080, null)
    }
  }

  override fun getItemCount() = dataItemCount + if (showLoadingMore) 1 else 0

  private val dataItemCount: Int
    get() = data.size

  private val loadingMoreItemPosition: Int
    get() = if (showLoadingMore) itemCount - 1 else RecyclerView.NO_POSITION

  override fun getItemViewType(position: Int): Int {
    if (position < dataItemCount && dataItemCount > 0) {
      return TYPE_ITEM
    }
    return TYPE_LOADING_MORE
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

  internal class ImageHolder(
    itemView: View,
    private val loadingPlaceholders: Array<ColorDrawable>
  ) :
    ViewHolder(itemView), BindableCatchUpItemViewHolder, TemporaryScopeHolder by temporaryScope() {

    internal var backingImageItem: ImageItem? = null
    internal val image: BadgedFourThreeImageView = itemView as BadgedFourThreeImageView

    override fun itemView(): View = itemView

    override fun bind(
      item: CatchUpItem,
      itemClickHandler: OnClickListener?,
      markClickHandler: OnClickListener?,
      longClickHandler: OnLongClickListener?
    ) {
      val scope = newScope()
      backingImageItem?.let { imageItem ->
        val (x, y) = imageItem.imageInfo.bestSize ?: Pair(image.measuredWidth, image.measuredHeight)
        GlideApp.with(itemView.context)
            .load(imageItem.imageInfo.url)
            .apply(RequestOptions()
                .placeholder(loadingPlaceholders[adapterPosition % loadingPlaceholders.size])
                .diskCacheStrategy(DiskCacheStrategy.DATA)
                .centerCrop()
                .override(x, y)
            )
            .transition(DrawableTransitionOptions.withCrossFade())
            .listener(object : RequestListener<Drawable> {
              override fun onResourceReady(
                resource: Drawable,
                model: Any,
                target: Target<Drawable>,
                dataSource: DataSource,
                isFirstResource: Boolean
              ): Boolean {
                itemView().setOnClickListener(itemClickHandler)
                itemView().setOnLongClickListener(longClickHandler)
                if (!imageItem.hasFadedIn) {
                  image.setHasTransientState(true)
                  val cm = ObservableColorMatrix()
                  // Saturation
                  ObjectAnimator.ofFloat(cm,
                      ObservableColorMatrix.SATURATION,
                      0f,
                      1f)
                      .apply {
                        addUpdateListener {
                          // just animating the color matrix does not invalidate the
                          // drawable so need this update listener.  Also have to create a
                          // new CMCF as the matrix is immutable :(
                          image.colorFilter = ColorMatrixColorFilter(cm)
                        }
                        duration = 2000L
                        interpolator = FastOutSlowInInterpolator()
                        doOnEnd {
                          image.clearColorFilter()
                          image.setHasTransientState(false)
                        }
                        start()
                        imageItem.hasFadedIn = true
                      }
                }
                return false
              }

              override fun onLoadFailed(
                e: GlideException?,
                model: Any,
                target: Target<Drawable>,
                isFirstResource: Boolean
              ) = false
            })
            .into(CatchUpTarget(image, false, scope))
            .clearOnDetach()
        // need both placeholder & background to prevent seeing through image as it fades in
        image.background = loadingPlaceholders[adapterPosition % loadingPlaceholders.size]
        image.showBadge(imageItem.imageInfo.animatable)
      }
    }
  }
}
