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

import android.view.View
import android.view.ViewGroup

/**
 * A [hierarchy change listener][android.view.ViewGroup.OnHierarchyChangeListener] which recursively
 * monitors an entire tree of views.
 */
class HierarchyTreeChangeListener private constructor(
  private val delegate: ViewGroup.OnHierarchyChangeListener
) :
  ViewGroup.OnHierarchyChangeListener {

  override fun onChildViewAdded(parent: View, child: View) {
    delegate.onChildViewAdded(parent, child)

    if (child is ViewGroup) {
      child.setOnHierarchyChangeListener(this)
      for (i in 0 until child.childCount) {
        onChildViewAdded(child, child.getChildAt(i))
      }
    }
  }

  override fun onChildViewRemoved(parent: View, child: View) {
    if (child is ViewGroup) {
      for (i in 0 until child.childCount) {
        onChildViewRemoved(child, child.getChildAt(i))
      }
      child.setOnHierarchyChangeListener(null)
    }

    delegate.onChildViewRemoved(parent, child)
  }

  companion object {

    /**
     * Wrap a regular [hierarchy change listener][android.view.ViewGroup.OnHierarchyChangeListener] with one
     * that monitors an entire tree of views.
     */
    fun wrap(delegate: ViewGroup.OnHierarchyChangeListener): HierarchyTreeChangeListener {
      return HierarchyTreeChangeListener(delegate)
    }
  }
}
