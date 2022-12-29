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
package io.sweers.catchup.base.ui

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import autodispose2.ScopeProvider
import autodispose2.androidx.lifecycle.scope
import io.reactivex.rxjava3.core.CompletableSource

abstract class BaseFragment<T : ViewBinding> : Fragment(), ScopeProvider, BackpressHandler {

  companion object {
    private val DAY_MODE_CONF = Configuration().apply { uiMode = Configuration.UI_MODE_NIGHT_NO }
  }

  @Suppress("LeakingThis") private lateinit var lifecycleProvider: ScopeProvider
  protected lateinit var binding: T
  protected var dayOnlyContext: Context? = null

  override fun onAttach(context: Context) {
    super.onAttach(context)
    dayOnlyContext = context.createConfigurationContext(DAY_MODE_CONF)
  }

  protected abstract val bindingInflater: (LayoutInflater, ViewGroup?, Boolean) -> T

  final override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    lifecycleProvider = viewLifecycleOwner.scope()
    return initView(inflater, container)
  }

  // TODO remove when compose fragment is separated
  protected open fun initView(inflater: LayoutInflater, container: ViewGroup?): View {
    binding = bindingInflater(inflater, container, false)
    return binding.root
  }

  override fun onDestroyView() {
    dayOnlyContext = null
    super.onDestroyView()
  }

  override fun requestScope(): CompletableSource {
    return lifecycleProvider.requestScope()
  }

  @Deprecated("Deprecated in Java")
  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    if (item.itemId == android.R.id.home) {
      return onBackPressed()
    }
    return super.onOptionsItemSelected(item)
  }

  override fun onBackPressed(): Boolean {
    childFragmentManager.fragments.filterIsInstance<BackpressHandler>().forEach {
      if (it.onBackPressed()) {
        return true
      }
    }
    return false
  }
}

interface BackpressHandler {
  fun onBackPressed(): Boolean
}
