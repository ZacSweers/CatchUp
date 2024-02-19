package catchup.app.data

import coil.intercept.Interceptor
import coil.request.ImageResult
import coil.size.pxOrElse
import okhttp3.HttpUrl.Companion.toHttpUrl

/** A Coil [Interceptor] which appends query params to Unsplash urls to request sized images. */
// TODO move this to just the dribbble subproject when coil's imageloader is exposed
object DribbbleSizingInterceptor : Interceptor {
  override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
    val data = chain.request.data
    val widthPx = chain.size.width.pxOrElse { -1 }
    val heightPx = chain.size.height.pxOrElse { -1 }
    if (
      widthPx > 0 &&
        heightPx > 0 &&
        data is String &&
        data.startsWith("https://cdn.dribbble.com/userupload")
    ) {
      val url =
        data.toHttpUrl().newBuilder().addQueryParameter("resize", "${widthPx}x${heightPx}").build()
      val request = chain.request.newBuilder().data(url).build()
      return chain.proceed(request)
    }
    return chain.proceed(chain.request)
  }
}
