package io.sweers.catchup.ui

import android.content.Context
import android.graphics.Typeface
import android.os.Handler
import android.os.HandlerThread
import android.support.v4.provider.FontRequest
import android.support.v4.provider.FontsContractCompat
import android.support.v4.provider.FontsContractCompat.FontRequestCallback
import io.sweers.catchup.R
import io.sweers.catchup.util.d
import io.sweers.catchup.util.e
import io.sweers.catchup.util.injection.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FontHelper @Inject constructor(@ApplicationContext context: Context) {

  private var font: Typeface? = null

  init {
    load(context)
  }

  fun load(context: Context) {
    d { "Downloading font" }
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
