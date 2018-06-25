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

package io.sweers.catchup.service.api

import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes

/**
 * A mark is a mark on the side of a catchup text item that can indicate some extra information,
 * such as a comment or delta or something that makes the reason for this item being here relevant
 * or important.
 *
 * Gross vars/constructors because of https://issuetracker.google.com/issues/67181813
 */
data class Mark(
    var text: String? = null,
    var textPrefix: String? = null,
    /**
     * By default, the icon used is a comment icon if this is null
     */
    @DrawableRes var icon: Int? = null,
    var clickUrl: String? = null,
    @ColorInt var iconTintColor: Int? = null,
    var formatTextAsCount: Boolean = false
) {
  constructor() : this(null, null, null, null, null, false)

  companion object {
    fun createCommentMark(count: Int, clickUrl: String? = null): Mark {
      return Mark(text = count.toString(),
          clickUrl = clickUrl,
          formatTextAsCount = true
      )
    }
  }
}
