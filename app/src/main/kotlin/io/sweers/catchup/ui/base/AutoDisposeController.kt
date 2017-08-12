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

import android.os.Bundle
import com.bluelinelabs.conductor.autodispose.ControllerEvent
import com.bluelinelabs.conductor.autodispose.ControllerEvent.DESTROY
import com.bluelinelabs.conductor.autodispose.ControllerEvent.DESTROY_VIEW
import com.bluelinelabs.conductor.autodispose.ControllerScopeProvider
import com.uber.autodispose.LifecycleScopeProvider
import io.reactivex.functions.Function
import io.sweers.catchup.rx.doOn

abstract class AutoDisposeController
  : RefWatchingController, LifecycleScopeProvider<ControllerEvent> {

  protected fun <T> T.doOnDestroyView(action: T.() -> Unit): T = apply {
    lifecycle().doOn(DESTROY_VIEW) { action() }
  }

  protected fun <T> T.doOnDestroy(action: T.() -> Unit): T = apply {
    lifecycle().doOn(DESTROY) { action() }
  }

  private val lifecycleProvider = ControllerScopeProvider.from(this)

  protected constructor() : super()

  protected constructor(args: Bundle) : super(args)

  override fun lifecycle(): io.reactivex.Observable<ControllerEvent> {
    return lifecycleProvider.lifecycle()
  }

  override fun correspondingEvents(): Function<ControllerEvent, ControllerEvent> {
    return lifecycleProvider.correspondingEvents()
  }

  override fun peekLifecycle(): ControllerEvent? {
    return lifecycleProvider.peekLifecycle()
  }
}
