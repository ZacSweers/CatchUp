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
package io.sweers.catchup.ui.debug

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import io.sweers.catchup.ui.BindableAdapter

internal class NetworkVarianceAdapter(context: Context) : BindableAdapter<Int>(context) {

  override fun getCount(): Int {
    return VALUES.size
  }

  override fun getItem(position: Int): Int {
    return VALUES[position]
  }

  override fun getItemId(position: Int): Long {
    return position.toLong()
  }

  override fun newView(inflater: LayoutInflater, position: Int, container: ViewGroup): View {
    return inflater.inflate(android.R.layout.simple_spinner_item, container, false)
  }

  override fun bindView(item: Int, position: Int, view: View) {
    val tv = view.findViewById<TextView>(android.R.id.text1)
    tv.text = "±$item%"
  }

  override fun newDropDownView(
    inflater: LayoutInflater,
    position: Int,
    container: ViewGroup
  ): View {
    return inflater.inflate(android.R.layout.simple_spinner_dropdown_item, container, false)
  }

  companion object {
    private val VALUES = intArrayOf(20, 40, 60)

    fun getPositionForValue(value: Int): Int {
      return VALUES.indices.firstOrNull { VALUES[it] == value }
          ?: 1 // Default to 40% if something changes.
    }
  }
}
