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
import android.content.Context
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
import io.sweers.catchup.util.resolveAttribute

/**
 * Controller base for different services.
 */
abstract class ServiceController : ButterKnifeController {
  @SuppressLint("SupportAnnotationUsage")
  @ColorInt var serviceThemeColor = Color.BLACK

  constructor() : super()

  constructor(args: Bundle) : super(args)

  override fun onContextAvailable(context: Context) {
    ConductorInjection.inject(this)
    super.onContextAvailable(context)
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View {
    requestThemedContext(container.context).let {
      if (container.context !== it) {
        serviceThemeColor = it.resolveAttribute(R.attr.colorAccent)
      }
    }
    return super.onCreateView(inflater, container)
  }

  fun <T> transformUrlToMeta(url: String?): ObservableTransformer<T, UrlMeta> {
    return ObservableTransformer { upstream ->
      upstream.map { UrlMeta(url, serviceThemeColor, activity!!) }
    }
  }

  class LoadingMoreHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    val progress: ProgressBar = itemView as ProgressBar

  }

  companion object {

    const val TYPE_ITEM = 0
    const val TYPE_LOADING_MORE = -1
  }
}
