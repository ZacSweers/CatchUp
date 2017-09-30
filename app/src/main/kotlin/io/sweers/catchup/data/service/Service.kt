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

package io.sweers.catchup.data.service

import android.content.Context
import android.net.Uri
import android.support.annotation.ColorInt
import android.support.annotation.ColorRes
import android.support.annotation.DrawableRes
import android.support.annotation.StringRes
import android.support.v4.content.ContextCompat
import android.view.View
import com.uber.autodispose.ScopeProvider
import com.uber.autodispose.kotlin.autoDisposeWith
import dagger.MapKey
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.functions.Function
import io.sweers.catchup.data.CatchUpItem

@MapKey
annotation class ServiceKey(val value: String)

@MapKey
annotation class ServiceMetaKey(val value: String)

interface Service {
  fun meta(): ServiceMeta
  fun fetchPage(request: DataRequest): Maybe<List<CatchUpItem>>
  fun bindItemView(item: CatchUpItem, holder: BindableCatchUpItemViewHolder)
  fun firstPageKey(): String
  fun linkHandler(): LinkHandler
  fun getNextPage(): String? = null
}

interface TextService : Service {
  override fun bindItemView(item: CatchUpItem, holder: BindableCatchUpItemViewHolder) {
    val context = holder.itemView().context
    val accentColor = ContextCompat.getColor(context, meta().themeColor)
    holder.tint(accentColor)
    holder.bind(
        item = item,
        linkHandler = linkHandler(),
        itemClickHandler = item.itemClickUrl?.let {
          { url: String ->
            holder.itemClicks()
                .map { UrlMeta(url, accentColor, context) }
                .flatMapCompletable(linkHandler())
                .autoDisposeWith(holder)
                .subscribe()
          }
        },
        commentClickHandler = item.itemCommentClickUrl?.let {
          { url: String ->
            holder.itemClicks()
                .map { UrlMeta(url, accentColor, context) }
                .flatMapCompletable(linkHandler())
                .autoDisposeWith(holder)
                .subscribe()
          }
        }
    )
    item.summarizationInfo?.let {
      holder.itemLongClicks()
          .autoDisposeWith(holder)
          .subscribe {
            // TODO Handle summarizations
          }
    }
  }
}

// TODO This'll one day give you an image based impl
interface VisualService : Service {
  override fun bindItemView(item: CatchUpItem, holder: BindableCatchUpItemViewHolder) {

  }
}

data class ServiceMeta(
    val id: String,
    @StringRes val name: Int,
    @ColorRes val themeColor: Int,
    @DrawableRes val icon: Int,
    val isVisual: Boolean = false
)

data class DataRequest(
    val fromRefresh: Boolean,
    val multiPage: Boolean,
    val pageId: String?
)

interface BindableCatchUpItemViewHolder : ScopeProvider {
  fun itemView(): View
  fun tint(@ColorInt color: Int)
  fun bind(item: CatchUpItem,
      linkHandler: LinkHandler,
      itemClickHandler: ((String) -> Any)? = null,
      commentClickHandler: ((String) -> Any)? = null)

  fun itemClicks(): Observable<Unit>
  fun itemLongClicks(): Observable<Unit>
  fun itemCommentClicks(): Observable<Unit>
}

interface LinkHandler : Function<UrlMeta, Completable>

data class UrlMeta(internal val uri: Uri?,
    @ColorInt internal val accentColor: Int,
    internal val context: Context) {

  constructor(url: String?, @ColorInt accentColor: Int, context: Context) : this(
      if (url.isNullOrBlank()) null else Uri.parse(url), accentColor, context)
}
