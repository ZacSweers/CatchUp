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

package io.sweers.catchup.service.api

import android.view.View
import androidx.annotation.ColorInt
import com.uber.autodispose.ScopeProvider
import io.reactivex.Observable

interface BindableCatchUpItemViewHolder : ScopeProvider {
  fun itemView(): View
  fun tint(@ColorInt color: Int) {
    // NOOP
  }

  fun bind(item: CatchUpItem,
      linkHandler: LinkHandler,
      itemClickHandler: ((String) -> Any)? = null,
      commentClickHandler: ((String) -> Any)? = null)

  fun itemClicks(): Observable<Unit> {
    TODO("itemClicks not implemented!")
  }

  fun itemLongClicks(): Observable<Unit> {
    TODO("itemLongClicks not implemented!")
  }

  fun itemCommentClicks(): Observable<Unit> {
    TODO("itemCommentClicks not implemented!")
  }
}
