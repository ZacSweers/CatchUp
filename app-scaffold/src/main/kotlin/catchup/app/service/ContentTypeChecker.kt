package catchup.app.service

import catchup.di.AppScope
import catchup.di.SingleIn
import catchup.service.api.ContentType
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
