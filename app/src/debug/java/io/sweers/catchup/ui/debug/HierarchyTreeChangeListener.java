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

package io.sweers.catchup.ui.debug;

import android.view.View;
import android.view.ViewGroup;

/**
 * A {@link android.view.ViewGroup.OnHierarchyChangeListener hierarchy change listener} which recursively
 * monitors an entire tree of views.
 */
public final class HierarchyTreeChangeListener implements ViewGroup.OnHierarchyChangeListener {
  private final ViewGroup.OnHierarchyChangeListener delegate;

  private HierarchyTreeChangeListener(ViewGroup.OnHierarchyChangeListener delegate) {
    if (delegate == null) {
      throw new NullPointerException("Delegate must not be null.");
    }
    this.delegate = delegate;
  }

  /**
   * Wrap a regular {@link android.view.ViewGroup.OnHierarchyChangeListener hierarchy change listener} with one
   * that monitors an entire tree of views.
   */
  public static HierarchyTreeChangeListener wrap(ViewGroup.OnHierarchyChangeListener delegate) {
    return new HierarchyTreeChangeListener(delegate);
  }

  @Override
  public void onChildViewAdded(View parent, View child) {
    delegate.onChildViewAdded(parent, child);

    if (child instanceof ViewGroup) {
      ViewGroup childGroup = (ViewGroup) child;
      childGroup.setOnHierarchyChangeListener(this);
      for (int i = 0; i < childGroup.getChildCount(); i++) {
        onChildViewAdded(childGroup, childGroup.getChildAt(i));
      }
    }
  }

  @Override
  public void onChildViewRemoved(View parent, View child) {
    if (child instanceof ViewGroup) {
      ViewGroup childGroup = (ViewGroup) child;
      for (int i = 0; i < childGroup.getChildCount(); i++) {
        onChildViewRemoved(childGroup, childGroup.getChildAt(i));
      }
      childGroup.setOnHierarchyChangeListener(null);
    }

    delegate.onChildViewRemoved(parent, child);
  }
}
