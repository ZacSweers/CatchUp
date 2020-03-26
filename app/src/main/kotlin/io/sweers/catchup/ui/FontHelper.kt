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
package io.sweers.catchup.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Handler
import android.os.HandlerThread
import androidx.core.provider.FontRequest
import androidx.core.provider.FontsContractCompat
import androidx.core.provider.FontsContractCompat.FontRequestCallback
import androidx.emoji.text.EmojiCompat
import androidx.emoji.text.EmojiCompat.InitCallback
import androidx.emoji.text.FontRequestEmojiCompatConfig
import io.sweers.catchup.R
import io.sweers.catchup.util.d
import io.sweers.catchup.util.e
import io.sweers.catchup.util.injection.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FontHelper @Inject constructor(
  @ApplicationContext context: Context,
  private val appConfig: dev.zacsweers.catchup.appconfig.AppConfig
) {

  private var font: Typeface? = null

  init {
    load(context)
  }

  fun load(context: Context) {
    d { "Downloading fonts" }
    val emojiRequest = FontRequest("com.google.android.gms.fonts",
        "com.google.android.gms",
        "Noto Color Emoji Compat",
        R.array.com_google_android_gms_fonts_certs)
    val emojiConfig = FontRequestEmojiCompatConfig(context, emojiRequest)
        .setEmojiSpanIndicatorEnabled(appConfig.isDebug)
        .setEmojiSpanIndicatorColor(Color.GREEN)
        .registerInitCallback(object : InitCallback() {
          override fun onInitialized() = d { "EmojiCompat initialized" }

          override fun onFailed(throwable: Throwable?) {
            e(throwable) { "EmojiCompat initialization failure." }
          }
        })
    EmojiCompat.init(emojiConfig)
    val request = FontRequest("com.google.android.gms.fonts",
        "com.google.android.gms",
        "Nunito",
        R.array.com_google_android_gms_fonts_certs)
    val callback = object : FontRequestCallback() {
      override fun onTypefaceRetrieved(typeface: Typeface) {
        d { "Font received" }
        font = typeface
      }

      override fun onTypefaceRequestFailed(reason: Int) {
        e { "Font download failed with reason $reason" }
      }
    }
    FontsContractCompat.requestFont(context.applicationContext,
        request,
        callback,
        Handler(HandlerThread("FontDownloader").apply { start() }.looper))
  }

  /**
   * Returns the font, or null if it's not present.
   */
  fun getFont() = font
}
