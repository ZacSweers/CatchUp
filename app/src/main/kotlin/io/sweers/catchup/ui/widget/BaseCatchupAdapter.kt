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
package io.sweers.catchup.ui.widget

import androidx.annotation.CallSuper
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import io.sweers.catchup.service.api.TemporaryScopeHolder

/** A base [Adapter] that handles common patterns, such as clearing scopes. */
abstract class BaseCatchupAdapter<VH : ViewHolder> : Adapter<VH>() {
  @CallSuper
  override fun onViewRecycled(holder: VH) {
    super.onViewRecycled(holder)
    if (holder is TemporaryScopeHolder) {
      holder.cancel()
    }
  }
}
