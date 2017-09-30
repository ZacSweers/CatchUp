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

package io.sweers.catchup.data

import android.content.Context
import android.support.v4.util.ArrayMap
import io.sweers.catchup.P
import io.sweers.catchup.data.designernews.DesignerNewsService
import io.sweers.catchup.data.dribbble.DribbbleService
import io.sweers.catchup.data.medium.MediumService
import io.sweers.catchup.data.model.ServiceData
import io.sweers.catchup.data.producthunt.ProductHuntService
import io.sweers.catchup.data.reddit.RedditService
import io.sweers.catchup.data.slashdot.SlashdotApi
import io.sweers.catchup.injection.qualifiers.ApplicationContext
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody
import okio.Okio
import java.io.IOException

/**
 * An interceptor that rewrites the response with mocked data instead.
 */
class MockDataInterceptor(@ApplicationContext private val context: Context) : Interceptor {

  @Throws(IOException::class)
  override fun intercept(chain: Interceptor.Chain): Response {
    val request = chain.request()
    val url = request.url()
    val host = url.host()
    val path = url.encodedPath()
    val serviceData = SUPPORTED_ENDPOINTS[host]
    return if (P.DebugMockModeEnabled.get() && serviceData != null && serviceData.supports(path)) {
      Response.Builder().request(request)
          .body(ResponseBody.create(
              MediaType.parse("application/json"),
              Okio.buffer(Okio.source(context.assets
                  .open(formatUrl(serviceData, url))))
                  .readUtf8()))
          .code(200)
          .protocol(Protocol.HTTP_1_1)
          .build()
    } else {
      chain.proceed(request)
    }
  }

  companion object {

    // TODO Generate this?
    private val SUPPORTED_ENDPOINTS = object : ArrayMap<String, ServiceData>() {
      init {
        put(RedditService.HOST,
            ServiceData.Builder("r").addEndpoint("/")
                .build())
        put(MediumService.HOST,
            ServiceData.Builder("m").addEndpoint("/browse/top")
                .build())
        put(ProductHuntService.HOST,
            ServiceData.Builder("ph").addEndpoint("/v1/posts")
                .build())
        put(SlashdotApi.HOST,
            ServiceData.Builder("sd").addEndpoint("/Slashdot/slashdotMainatom")
                .fileType("xml")
                .build())
        put(DesignerNewsService.HOST,
            ServiceData.Builder("dn").addEndpoint("/api/v1/stories")
                .build())
        put(DribbbleService.HOST,
            ServiceData.Builder("dr").addEndpoint("/v1/shots")
                .build())
      }
    }

    private fun formatUrl(service: ServiceData, url: HttpUrl): String {
      var lastSegment = url.pathSegments()[url.pathSize() - 1]
      if ("" == lastSegment) {
        lastSegment = "nopath"
      }
      return service.assetsPrefix + "/" + lastSegment + "." + service.fileType
    }
  }
}
