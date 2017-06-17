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
import android.view.View
import com.bluelinelabs.conductor.Controller
import com.uber.autodispose.LifecycleScopeProvider
import io.reactivex.functions.Function
import io.reactivex.subjects.BehaviorSubject

abstract class AutoDisposeController : RefWatchingController, LifecycleScopeProvider<ControllerEvent> {

  private val lifecycleSubject = BehaviorSubject.createDefault(ControllerEvent.CREATE)

  protected constructor() : super() {
    initLifecycleHandling()
  }

  protected constructor(args: Bundle) : super(args) {
    initLifecycleHandling()
  }

  private fun initLifecycleHandling() {
    addLifecycleListener(object : Controller.LifecycleListener() {
      override fun preCreateView(controller: Controller) {
        lifecycleSubject.onNext(ControllerEvent.CREATE_VIEW)
      }

      override fun preAttach(controller: Controller, view: View) {
        lifecycleSubject.onNext(ControllerEvent.ATTACH)
      }

      override fun preDetach(controller: Controller, view: View) {
        lifecycleSubject.onNext(ControllerEvent.DETACH)
      }

      override fun preDestroyView(controller: Controller, view: View) {
        lifecycleSubject.onNext(ControllerEvent.DESTROY_VIEW)
      }

      override fun preDestroy(controller: Controller) {
        lifecycleSubject.onNext(ControllerEvent.DESTROY)
      }
    })
  }

  override fun lifecycle(): io.reactivex.Observable<ControllerEvent> {
    return lifecycleSubject
  }

  override fun correspondingEvents(): Function<ControllerEvent, ControllerEvent> {
    return ControllerEvent.LIFECYCLE
  }

  override fun peekLifecycle(): ControllerEvent {
    return lifecycleSubject.value
  }
}
