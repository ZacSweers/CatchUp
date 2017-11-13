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

package io.sweers.catchup.service.api

import android.arch.persistence.room.Embedded
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import android.support.annotation.Keep
import org.threeten.bp.Instant

// Gross vars/constructors because of https://issuetracker.google.com/issues/67181813
@Keep
@Entity(tableName = "items")
data class CatchUpItem(
    @PrimaryKey var id: Long,
    var title: String,
    var timestamp: Instant,
    var score: Pair<String, Int>? = null,
    var tag: String? = null,
    var author: String? = null,
    var source: String? = null,
    var commentCount: Int = 0,
    var hideComments: Boolean = false,
    var itemClickUrl: String? = null,
    var itemCommentClickUrl: String? = null,
    @Embedded var summarizationInfo: SummarizationInfo? = null,
    @Embedded var imageInfo: ImageInfo? = null
) : DisplayableItem {
  constructor() : this(0, "", Instant.now())

  override fun stableId() = id

  override fun realItem() = this
}

