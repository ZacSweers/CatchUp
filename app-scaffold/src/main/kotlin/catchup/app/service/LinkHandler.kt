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
package catchup.app.service

import android.net.Uri
import androidx.annotation.CheckResult
import androidx.annotation.ColorInt
import androidx.compose.ui.graphics.Color
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl

interface LinkHandler {
  @CheckResult
  suspend fun openUrl(url: HttpUrl, @ColorInt accentColor: Color = Color.Unspecified): Boolean

  @CheckResult fun shareUrl(url: HttpUrl, title: String? = null): Boolean
}

suspend fun LinkHandler.openUrl(
  url: String,
  @ColorInt accentColor: Color = Color.Unspecified,
): Boolean {
  return openUrl(url.toHttpUrl(), accentColor)
}

suspend fun LinkHandler.openUrl(
  uri: Uri,
  @ColorInt accentColor: Color = Color.Unspecified,
): Boolean {
  return openUrl(uri.toString(), accentColor)
}

fun LinkHandler.shareUrl(url: String, title: String? = null): Boolean {
  return shareUrl(url.toHttpUrl(), title)
}
