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

package io.sweers.catchup.injection

import com.bluelinelabs.conductor.Controller
import dagger.android.AndroidInjector
import dagger.android.HasActivityInjector
import dagger.internal.Preconditions.checkNotNull
import io.sweers.catchup.util.d

/** Injects core Conductor types.  */
object ConductorInjection {

  /**
   * Injects `controller` if an associated [AndroidInjector.Factory] implementation can be
   * found, otherwise throws an [IllegalArgumentException].
   *
   * Uses the following algorithm to find the appropriate `DispatchingAndroidInjector<Controller>` to inject `controller`:
   *
   *  1. Walks the parent-controller hierarchy to find the a controller that implements [HasControllerInjector], and if none do
   *  2. Uses the `controller`'s [activity][Controller.getActivity] if it implements
   * [HasControllerInjector], and if not
   *  3. Uses the [android.app.Application] if it implements [HasActivityInjector].
   *
   * If none of them implement [HasControllerInjector], a [IllegalArgumentException] is thrown.
   *
   * @throws IllegalArgumentException if no `AndroidInjector.Factory<Controller, ?>` is bound
   * for `controller`.
   */
  fun inject(controller: Controller) {
    val hasDispatchingControllerInjector = findHasControllerInjector(controller)
    d {
      String.format(
          "An injector for %s was found in %s",
          controller.javaClass
              .canonicalName,
          hasDispatchingControllerInjector.javaClass
              .canonicalName)
    }

    val controllerInjector = hasDispatchingControllerInjector.controllerInjector()
    checkNotNull(
        controllerInjector,
        "%s.controllerInjector() returned null",
        hasDispatchingControllerInjector.javaClass
            .canonicalName)

    controllerInjector.inject(controller)
  }

  private fun findHasControllerInjector(controller: Controller): HasControllerInjector {
    val controllerSequence = generateSequence(controller) { it.parentController } // <3 Kotlin
    controllerSequence.forEach {
      if (it is HasControllerInjector) {
        return it
      }
    }
    controller.activity?.let {
      if (it is HasControllerInjector) {
        return it
      } else if (it.application is HasControllerInjector) {
        return it.application as HasControllerInjector
      }
    }
    throw IllegalArgumentException(String.format(
        "No injector was found for %s",
        controller.javaClass
            .canonicalName))
  }
}
