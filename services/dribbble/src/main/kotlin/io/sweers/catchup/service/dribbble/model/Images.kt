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

package io.sweers.catchup.service.dribbble.model

/**
 * Models links to the various quality of images of a shot.
 */
internal data class Images(val hidpi: String?,
    val normal: String,
    val teaser: String?) {

  fun best(): String = hidpi ?: normal

  fun bestSize(): Pair<Int, Int> = hidpi?.let { TWO_X_IMAGE_SIZE } ?: NORMAL_IMAGE_SIZE

  companion object {
    private val NORMAL_IMAGE_SIZE = 400 to 300
    private val TWO_X_IMAGE_SIZE = 800 to 600
  }

}
