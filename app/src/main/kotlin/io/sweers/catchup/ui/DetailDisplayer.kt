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
package io.sweers.catchup.ui

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import me.saket.inboxrecyclerview.InboxRecyclerView
import me.saket.inboxrecyclerview.page.ExpandablePageLayout

interface DetailDisplayer {
  val isExpandedOrExpanding: Boolean

  /**
   * Shows a detail activity given an available page and fragmentmanager.
   */
  fun showDetail(body: (ExpandablePageLayout, FragmentManager) -> () -> Unit)

  /**
   * Binds (via [InboxRecyclerView.expandablePage] an irv to the available page/fragment only,
   * does nothing with [FragmentManager]. Used mostly for state restoration.
   *
   * If [useExistingFragment], it should be resolved from a [FragmentManager] and call through to
   * [bind] with the result.
   */
  fun bind(irv: InboxRecyclerView, useExistingFragment: Boolean = false)

  /**
   * Binds (via [InboxRecyclerView.expandablePage] an irv to the available page/fragment only,
   * does nothing with [FragmentManager]. Used mostly for state restoration. [bind] can call this,
   * but this should not call [bind].
   */
  fun bind(irv: InboxRecyclerView, targetFragment: Fragment?)

  /**
   * Unbinds (via [InboxRecyclerView.expandablePage] from an irv to the available page only, does nothing
   * with fragmentmanagers. Used mostly for state restoration.
   */
  fun unbind(irv: InboxRecyclerView)
}
