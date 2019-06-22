/*
 * Copyright (c) 2019 Zac Sweers
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

package io.sweers.catchup.flowbinding

import com.google.android.material.appbar.AppBarLayout
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate

fun AppBarLayout.offsetChanges(): Flow<Int> = callbackFlow<Int> {
  val listener = AppBarLayout.OnOffsetChangedListener { _, verticalOffset ->
    safeOffer(verticalOffset)
  }
  addOnOffsetChangedListener(listener)
  awaitClose {
    removeOnOffsetChangedListener(listener)
  }
}.conflate()
