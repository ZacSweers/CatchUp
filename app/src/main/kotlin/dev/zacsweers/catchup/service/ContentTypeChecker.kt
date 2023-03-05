package dev.zacsweers.catchup.service

import dev.zacsweers.catchup.di.AppScope
import dev.zacsweers.catchup.di.SingleIn
import io.sweers.catchup.service.api.ContentType
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

@SingleIn(AppScope::class)
class ContentTypeChecker @Inject constructor(private val okHttpClient: OkHttpClient) {
  suspend fun contentType(url: String): ContentType? {
    // Fast-path basic media checks
    if (url.hasMediaExtension) {
      return ContentType.IMAGE
    }
    val response =
      withContext(IO) {
        try {
          okHttpClient.newCall(Request.Builder().url(url).head().build()).execute()
        } catch (e: IOException) {
          return@withContext null
        }
      }
        ?: return null
    return if (response.isSuccessful) {
      response.header("Content-Type")?.contentType()
    } else {
      null
    }
  }
}

private fun String?.contentType(): ContentType? {
  return when {
    this == null -> null
    startsWith("image/") -> ContentType.IMAGE
    startsWith("text/html") -> ContentType.HTML
    else -> ContentType.OTHER
  }
}

private val MEDIA_EXTENSIONS =
  setOf(
    "apng",
    "avif",
    "gif",
    "jpeg",
    "png",
    "webp",
  )

private val String.hasMediaExtension: Boolean
  get() {
    return substring(lastIndexOf(".") + 1) in MEDIA_EXTENSIONS
  }
