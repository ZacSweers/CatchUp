/*
 * Copyright (C) 2026. Zac Sweers
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
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
package catchup.app.service

import catchup.service.api.ContentType
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

@SingleIn(AppScope::class)
@Inject
class ContentTypeChecker {
  fun contentType(url: String): ContentType {
    // Fast-path basic media checks
    if (url.hasMediaExtension) {
      return ContentType.IMAGE
    }
    if (url.isMediaUrl) {
      return ContentType.IMAGE
    }

    return ContentType.OTHER
  }
}

private val MEDIA_EXTENSIONS = setOf("apng", "avif", "gif", "jpeg", "png", "webp")

private val String.hasMediaExtension: Boolean
  get() {
    return substring(lastIndexOf(".") + 1) in MEDIA_EXTENSIONS
  }

private val KNOWN_DOMAINS = setOf("i.imgur.com", "i.redd.it")

private val String.isMediaUrl: Boolean
  get() {
    return toHttpUrlOrNull()?.host?.let { it in KNOWN_DOMAINS } ?: false
  }
