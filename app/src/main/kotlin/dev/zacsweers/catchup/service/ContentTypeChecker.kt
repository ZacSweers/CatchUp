package dev.zacsweers.catchup.service

import dev.zacsweers.catchup.di.AppScope
import dev.zacsweers.catchup.di.SingleIn
import io.sweers.catchup.service.api.ContentType
import javax.inject.Inject
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

@SingleIn(AppScope::class)
class ContentTypeChecker @Inject constructor() {
  fun contentType(url: String): ContentType {
    // Fast-path basic media checks
    if (url.hasMediaExtension) {
      return ContentType.IMAGE
    }
    if (url.isMediaUrl) {
      return ContentType.IMAGE
    }

    return ContentType.OTHER
    // TODO the below is too slow to use tbh, need a better alternative
    //    val response =
    //      withContext(IO) {
    //        try {
    //          okHttpClient.get().newCall(Request.Builder().url(url).head().build()).execute()
    //        } catch (e: IOException) {
    //          return@withContext null
    //        }
    //      }
    //        ?: return null
    //    return if (response.isSuccessful) {
    //      response.header("Content-Type")?.contentType()
    //    } else {
    //      null
    //    }
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

private val KNOWN_DOMAINS =
  setOf(
    "i.imgur.com",
    "i.redd.it",
  )

private val String.isMediaUrl: Boolean
  get() {
    return toHttpUrlOrNull()?.host?.let { it in KNOWN_DOMAINS } ?: false
  }
