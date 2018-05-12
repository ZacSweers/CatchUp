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

import android.os.Bundle
import com.bluelinelabs.conductor.autodispose.ControllerEvent
import com.bluelinelabs.conductor.autodispose.ControllerEvent.DESTROY
import com.bluelinelabs.conductor.autodispose.ControllerEvent.DESTROY_VIEW
import com.bluelinelabs.conductor.autodispose.ControllerScopeProvider
import com.uber.autodispose.LifecycleScopeProvider
import io.reactivex.Observable
import io.reactivex.functions.Function

abstract class AutoDisposeController
  : RefWatchingController, LifecycleScopeProvider<ControllerEvent> {

  @Suppress("LeakingThis")
  private val lifecycleProvider = ControllerScopeProvider.from(this)

  protected inline fun <T, R> Observable<T>.doOnDestroyView(r: R,
      crossinline action: R.() -> Unit): Observable<T> = apply {
    doOnNext {
      if (it == DESTROY_VIEW) {
        r.action()
      }
    }
  }

  protected inline fun <T, R> Observable<T>.doOnDestroy(r: R,
      crossinline action: R.() -> Unit): Observable<T> = apply {
    doOnNext {
      if (it == DESTROY) {
        r.action()
      }
    }
  }

  protected inline fun <T> T.doOnDestroyView(crossinline action: T.() -> Unit): T = apply {
    lifecycle().doOnDestroyView(this) { action() }.subscribe()
  }

  protected inline fun <T> T.doOnDestroy(crossinline action: T.() -> Unit): T = apply {
    lifecycle().doOnDestroy(this) { action() }.subscribe()
  }

  protected constructor() : super()

  protected constructor(args: Bundle) : super(args)

  override final fun lifecycle(): io.reactivex.Observable<ControllerEvent> {
    return lifecycleProvider.lifecycle()
  }

  override final fun correspondingEvents(): Function<ControllerEvent, ControllerEvent> {
    return lifecycleProvider.correspondingEvents()
  }

  override final fun peekLifecycle(): ControllerEvent? {
    return lifecycleProvider.peekLifecycle()
  }
}
