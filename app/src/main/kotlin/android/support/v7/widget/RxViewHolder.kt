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

@file:Suppress("PackageDirectoryMismatch")

package androidx.recyclerview.widget

import android.view.View
import com.uber.autodispose.ScopeProvider
import io.reactivex.Maybe
import io.reactivex.subjects.MaybeSubject

private object NOTIFICATION

abstract class RxViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), ScopeProvider {

  private var unbindNotifier: MaybeSubject<Any>? = null

  private val notifier: MaybeSubject<Any>
    get() {
      synchronized(this) {
        var n = unbindNotifier
        return if (n == null) {
          n = MaybeSubject.create<Any>()
          unbindNotifier = n
          n
        } else {
          n
        }
      }
    }

  private fun onUnBind() {
    emitUnBindIfPresent()
    unbindNotifier = null
  }

  private fun emitUnBindIfPresent() {
    unbindNotifier?.let {
      if (!it.hasComplete()) {
        it.onSuccess(NOTIFICATION)
      }
    }
  }

  override fun requestScope(): Maybe<*> {
    return notifier
  }

  internal override fun setFlags(flags: Int, mask: Int) {
    val wasBound = isBound
    super.setFlags(flags, mask)
    if (wasBound && !isBound) {
      onUnBind()
    }
  }

  internal override fun addFlags(flags: Int) {
    val wasBound = isBound
    super.addFlags(flags)
    if (wasBound && !isBound) {
      onUnBind()
    }
  }

  internal override fun clearPayload() {
    val wasBound = isBound
    super.clearPayload()
    if (wasBound && !isBound) {
      onUnBind()
    }
  }

  internal override fun resetInternal() {
    val wasBound = isBound
    super.resetInternal()
    if (wasBound && !isBound) {
      onUnBind()
    }
  }
}
