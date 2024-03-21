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
package catchup.service.api

import androidx.annotation.ColorInt
import androidx.annotation.Keep
import androidx.compose.runtime.Immutable
import catchup.service.api.ContentType.HTML
import catchup.service.api.Mark.MarkType
import java.util.Objects
import kotlinx.datetime.Instant

@Keep
@Immutable
data class CatchUpItem(
  val id: Long,
  val title: String,
  val description: String? = null,
  val timestamp: Instant? = null,
  val score: Pair<String, Int>? = null,
  val tag: String? = null,
  @ColorInt val tagHintColor: Int? = null,
  val author: String? = null,
  val source: String? = null,
  /* internal */ val itemClickUrl: String? = null,
  val imageInfo: ImageInfo? = null,
  val mark: Mark? = null,
  val detailKey: String? = null,
  val serviceId: String? = null,
  val indexInResponse: Int? = null,
  val contentType: ContentType? = null, // Null indicates unset, try to infer it
) {

  val clickUrl: String?
  val markClickUrl: String?

  init {
    val markClickUrl = mark?._markClickUrl
    val itemClickUrl = itemClickUrl ?: markClickUrl
    val finalMarkClickUrl = markClickUrl?.let { if (itemClickUrl == markClickUrl) null else it }
    this.clickUrl = itemClickUrl
    this.markClickUrl = finalMarkClickUrl
  }

  companion object {
    fun fakeItems(count: Int, serviceId: String, isImage: Boolean): List<CatchUpItem> {
      return (0 until count).map { fake(it, serviceId, isImage) }
    }

    fun fake(
      index: Int,
      serviceId: String,
      isImage: Boolean,
      id: Long = Objects.hash(index, serviceId).toLong(),
    ): CatchUpItem {
      val imageInfo =
        if (isImage) {
          val url = "https://picsum.photos/seed/$index/300/300"
          ImageInfo(
            url = url,
            bestSize = 400 to 300,
            animatable = false,
            detailUrl = url,
            sourceUrl = url,
            aspectRatio = 4 / 3f,
            imageId = url,
          )
        } else {
          null
        }

      return CatchUpItem(
        id = id,
        title = "Lorem ipsum dolor sit amet",
        description =
          "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.",
        timestamp = Instant.parse("2020-01-01T00:00:00Z"),
        score = "+" to 5,
        tag = "Tag",
        author = "Author",
        source = "Source",
        itemClickUrl = "https://example.com",
        imageInfo = imageInfo,
        mark = Mark(markType = MarkType.COMMENT, _markClickUrl = "https://example.com"),
        detailKey = "0",
        serviceId = serviceId,
        indexInResponse = index,
        contentType = HTML,
      )
    }
  }
}

val CatchUpItem.canBeSummarized: Boolean
  get() = contentType == HTML

val CatchUpItem.supportsDetail: Boolean
  get() = detailKey != null
