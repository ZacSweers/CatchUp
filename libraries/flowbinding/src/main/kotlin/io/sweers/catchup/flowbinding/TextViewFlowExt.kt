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

import android.text.Editable
import android.text.TextWatcher
import android.widget.TextView
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate

data class TextViewAfterTextChangeEvent(val textView: TextView, val editable: Editable?)

// TODO Remove after it's public in corbinding: https://github.com/LDRAlighieri/Corbind/pull/1
fun TextView.afterTextChangeEvents(): Flow<TextViewAfterTextChangeEvent> = callbackFlow<TextViewAfterTextChangeEvent> {
  val watcher = object : TextWatcher {
    override fun afterTextChanged(s: Editable?) {
      safeOffer(TextViewAfterTextChangeEvent(this@afterTextChangeEvents, s))
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
    }
  }
  addTextChangedListener(watcher)
  awaitClose {
    removeTextChangedListener(watcher)
  }
}.conflate()
