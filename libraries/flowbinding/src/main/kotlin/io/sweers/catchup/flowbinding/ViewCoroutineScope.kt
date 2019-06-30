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
package io.sweers.catchup.flowbinding

import android.view.View
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlin.coroutines.CoroutineContext

private val VIEW_KEY = "ViewCoroutineScope".hashCode()

/**
 * [CoroutineScope] tied to this [View].
 * This scope will be canceled when the view is detached.
 *
 * This scope is bound to [Dispatchers.Main]
 */
fun View.viewScope(allowOnUnAttach: Boolean = true): CoroutineScope {
  if (!allowOnUnAttach && !isAttachedToWindow) {
    error("View is not attached!")
  }
  getTag(VIEW_KEY)?.let {
    if (it is CoroutineScope) {
      return it
    }
  }

  val scope = ViewCoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
  val listener = object : View.OnAttachStateChangeListener {
    override fun onViewDetachedFromWindow(v: View) {
      scope.coroutineContext.cancel()
      removeOnAttachStateChangeListener(this)
    }

    override fun onViewAttachedToWindow(v: View) {
    }
  }
  setTag(VIEW_KEY, scope)
  addOnAttachStateChangeListener(listener)
  return scope
}

internal class ViewCoroutineScope(context: CoroutineContext) : CoroutineScope {
  override val coroutineContext: CoroutineContext = context
}
