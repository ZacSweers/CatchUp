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

import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes

/**
 * A mark is a mark on the side of a catchup text item that can indicate some extra information,
 * such as a comment or delta or something that makes the reason for this item being here relevant
 * or important.
 */
data class Mark(
  val text: String? = null,
  val textPrefix: String? = null,
  /**
   * By default, the icon used is a comment icon if this is null
   */
  @DrawableRes val icon: Int? = null,
  val clickUrl: String? = null,
  @ColorInt val iconTintColor: Int? = null,
  val formatTextAsCount: Boolean = false
) {

  companion object {
    fun createCommentMark(count: Int, clickUrl: String? = null): Mark {
      return Mark(text = count.toString(),
          clickUrl = clickUrl,
          formatTextAsCount = true
      )
    }
  }
}
