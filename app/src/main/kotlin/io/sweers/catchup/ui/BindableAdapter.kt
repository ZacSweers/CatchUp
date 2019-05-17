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
package io.sweers.catchup.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter

/**
 * An implementation of [BaseAdapter] which uses the bind pattern for its views.
 */
abstract class BindableAdapter<T>(val context: Context) : BaseAdapter() {
  private val inflater: LayoutInflater = LayoutInflater.from(context)

  abstract override fun getItem(position: Int): T

  override fun getView(position: Int, convertView: View?, container: ViewGroup): View {
    val view = convertView ?: newView(inflater, position, container)
    bindView(getItem(position), position, view)
    return view
  }

  /**
   * Create a new instance of a view for the specified position.
   */
  abstract fun newView(inflater: LayoutInflater, position: Int, container: ViewGroup): View

  /**
   * Bind the data for the specified `position` to the view.
   */
  abstract fun bindView(item: T, position: Int, view: View)

  override fun getDropDownView(position: Int, convertView: View?, container: ViewGroup): View {
    val view = convertView ?: newDropDownView(inflater, position, container)
    bindDropDownView(getItem(position), position, view)
    return view
  }

  /**
   * Create a new instance of a drop-down view for the specified position.
   */
  open fun newDropDownView(inflater: LayoutInflater, position: Int, container: ViewGroup): View {
    return newView(inflater, position, container)
  }

  /**
   * Bind the data for the specified `position` to the drop-down view.
   */
  private fun bindDropDownView(item: T, position: Int, view: View) {
    bindView(item, position, view)
  }
}
