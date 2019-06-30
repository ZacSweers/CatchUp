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
package io.sweers.catchup.service.api

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlin.coroutines.CoroutineContext

/**
 * Represents a an entity that holds a temporary [CoroutineScope]. Can be used in delegation to manage
 * scopes and request new ones as needed via [temporaryScope].
 */
interface TemporaryScopeHolder {
  val currentScope: CoroutineScope?
  fun newScope(): CoroutineScope
  fun cancel()
}

fun temporaryScope(): TemporaryScopeHolder = TemporaryScopeDelegate()

internal class TemporaryScopeDelegate : TemporaryScopeHolder {

  override var currentScope: CoroutineScope? = null

  override fun newScope(): CoroutineScope {
    currentScope?.coroutineContext?.cancel()
    return object : CoroutineScope {
      override val coroutineContext: CoroutineContext = SupervisorJob() + Dispatchers.Main.immediate
    }.also {
      currentScope = it
    }
  }

  override fun cancel() {
    currentScope?.cancel()
  }
}
