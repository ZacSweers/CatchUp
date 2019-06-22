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

import android.view.View
import android.widget.Adapter
import android.widget.AdapterView
import android.widget.AdapterView.INVALID_POSITION
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate

fun <T : Adapter> AdapterView<T>.itemSelections(
    offerInitial: Boolean = true): Flow<Int> = callbackFlow<Int> {
  val listener = object : AdapterView.OnItemSelectedListener {
    override fun onNothingSelected(parent: AdapterView<*>?) {
      safeOffer(INVALID_POSITION)
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
      safeOffer(position)
    }
  }
  onItemSelectedListener = listener
  if (offerInitial) {
    safeOffer(selectedItemPosition)
  }
  awaitClose {
    onItemSelectedListener = null
  }
}.conflate()
