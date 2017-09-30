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

import android.support.annotation.ColorRes
import android.support.annotation.DrawableRes
import android.support.annotation.StringRes
import android.support.v4.content.ContextCompat
import com.uber.autodispose.kotlin.autoDisposeWith
import dagger.MapKey
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.functions.Function
import io.sweers.catchup.data.CatchUpItem
import io.sweers.catchup.data.LinkManager
import io.sweers.catchup.data.LinkManager.UrlMeta
import io.sweers.catchup.ui.base.CatchUpItemViewHolder


private val completableFunc = Function<UrlMeta, Completable> { Completable.complete() }

@MapKey
annotation class ServiceKey(val value: String)

@MapKey
annotation class ServiceMetaKey(val value: String)

interface Service<in ViewHolderType> {
  fun meta(): ServiceMeta
  fun fetchPage(request: DataRequest): Maybe<List<CatchUpItem>>
  fun bindItemView(item: CatchUpItem, holder: ViewHolderType)
  fun setNextPage(page: String?) {}
  fun linkManager(): LinkManager? = null
}

interface TextService : Service<CatchUpItemViewHolder> {
  override fun bindItemView(item: CatchUpItem, holder: CatchUpItemViewHolder) {
    val context = holder.itemView.context
    val accentColor = ContextCompat.getColor(context, meta().themeColor)
    holder.tint(accentColor)
    holder.bind(
        controller = null,
        item = item,
        linkManager = null,
        itemClickHandler = item.itemClickUrl?.let {
          { url: String ->
            holder.itemClicks()
                .map { UrlMeta(url, accentColor, context) }
                .flatMapCompletable(linkManager() ?: completableFunc)
                .autoDisposeWith(holder)
                .subscribe()
          }
        },
        commentClickHandler = item.itemCommentClickUrl?.let {
          { url: String ->
            holder.itemClicks()
                .map { UrlMeta(url, accentColor, context) }
                .flatMapCompletable(linkManager() ?: completableFunc)
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
interface VisualService : Service<CatchUpItemViewHolder> {
  override fun bindItemView(item: CatchUpItem, holder: CatchUpItemViewHolder) {

  }
}

data class ServiceMeta(
    val id: String,
    @StringRes val name: Int,
    @ColorRes val themeColor: Int,
    @DrawableRes val icon: Int
)

data class DataRequest(
    val fromRefresh: Boolean,
    val multipage: Boolean,
    val pageId: String?
)
