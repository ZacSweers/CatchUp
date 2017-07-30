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

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import butterknife.Unbinder

abstract class ButterKnifeController : AutoDisposeController {

  private var themedContext: Context? = null
  private var unbinder: Unbinder? = null

  protected constructor() : super()

  protected constructor(args: Bundle) : super(args)

  protected abstract fun inflateView(inflater: LayoutInflater,
      container: ViewGroup): View

  /**
   * Callback for wrapping context. Override for your own theme. Result will be cached
   */
  protected open fun onThemeContext(context: Context): Context {
    return context
  }

  protected fun requestThemedContext(context: Context): Context {
    return themedContext ?: onThemeContext(context).also { themedContext = it }
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View {
    val themedContext = requestThemedContext(container.context)
    val view = inflateView(LayoutInflater.from(themedContext), container)
    unbinder = bind(view)
    onViewBound(view)
    return view
  }

  protected abstract fun bind(view: View): Unbinder

  protected open fun onViewBound(view: View) {}

  override fun onDestroyView(view: View) {
    unbinder?.unbind()
    themedContext = null
    super.onDestroyView(view)
  }
}
