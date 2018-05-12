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

package io.sweers.catchup.ui.base

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import butterknife.Unbinder

abstract class ButterKnifeController : AutoDisposeController {

  companion object {
    private val DAY_MODE_CONF = Configuration().apply {
      uiMode = Configuration.UI_MODE_NIGHT_NO
    }
  }

  protected var dayOnlyContext: Context? = null

  protected constructor() : super()

  protected constructor(args: Bundle) : super(args)

  override fun onContextAvailable(context: Context) {
    super.onContextAvailable(context)
    dayOnlyContext = context.createConfigurationContext(DAY_MODE_CONF)
  }

  protected abstract fun inflateView(inflater: LayoutInflater,
      container: ViewGroup): View

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View {
    val view = inflateView(LayoutInflater.from(container.context), container)
    bind(view).doOnDestroyView { unbind() }
    onViewBound(view)
    return view
  }

  protected abstract fun bind(view: View): Unbinder

  protected open fun onViewBound(view: View) {}

  override fun onDestroyView(view: View) {
    dayOnlyContext = null
    super.onDestroyView(view)
  }
}
