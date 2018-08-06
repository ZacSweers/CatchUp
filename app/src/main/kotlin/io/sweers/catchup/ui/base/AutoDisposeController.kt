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
import com.uber.autodispose.lifecycle.CorrespondingEventsFunction
import com.uber.autodispose.lifecycle.KotlinLifecycleScopeProvider
import io.reactivex.Observable

abstract class AutoDisposeController
  : RefWatchingController, KotlinLifecycleScopeProvider<ControllerEvent> {

  @Suppress("LeakingThis")
  private val lifecycleProvider = KotlinControllerScopeProvider.from(this)

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

  final override fun lifecycle(): io.reactivex.Observable<ControllerEvent> {
    return lifecycleProvider.lifecycle()
  }

  final override fun correspondingEvents(): CorrespondingEventsFunction<ControllerEvent> {
    return CorrespondingEventsFunction {
      lifecycleProvider.correspondingEvents().apply(it)
    }
  }

  final override fun peekLifecycle(): ControllerEvent? {
    return lifecycleProvider.peekLifecycle()
  }
}
