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

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.support.annotation.ColorInt
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import io.reactivex.ObservableTransformer
import io.sweers.catchup.R
import io.sweers.catchup.data.LinkManager.UrlMeta
import io.sweers.catchup.injection.ConductorInjection
import io.sweers.catchup.util.UiUtil

/**
 * Controller base for different services.
 */
abstract class ServiceController : ButterKnifeController {
  @SuppressLint("SupportAnnotationUsage")
  @ColorInt var serviceThemeColor = Color.BLACK

  constructor() : super()

  constructor(args: Bundle) : super(args)

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View {
    val view = super.onCreateView(inflater, container)
    val themedContext = view.context
    if (container.context !== themedContext) {
      serviceThemeColor = UiUtil.resolveAttribute(themedContext, R.attr.colorAccent)
    }
    return view
  }

  override fun onViewBound(view: View) {
    ConductorInjection.inject(this)
    super.onViewBound(view)
  }

  protected fun <T> transformUrlToMeta(url: String?): ObservableTransformer<T, UrlMeta> {
    return ObservableTransformer { upstream ->
      upstream.map { UrlMeta(url, serviceThemeColor, activity!!) }
    }
  }

  class LoadingMoreHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    val progress: ProgressBar = itemView as ProgressBar

  }

  companion object {

    @JvmStatic val TYPE_ITEM = 0
    @JvmStatic val TYPE_LOADING_MORE = -1
  }
}
