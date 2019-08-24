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
package io.sweers.catchup.base.ui

import android.view.ViewGroup

/**
 * An indirection which allows controlling the root container used for each activity.
 */
interface ViewContainer {

  /**
   * The root [ViewGroup] into which the activity should place its contents.
   */
  fun forActivity(activity: BaseActivity): ViewGroup

  companion object {
    /**
     * An [ViewContainer] which returns the normal activity content view.
     */
    val DEFAULT = object : ViewContainer {
      override fun forActivity(activity: BaseActivity): ViewGroup {
        return activity.findViewById(android.R.id.content)
      }
    }
  }
}
