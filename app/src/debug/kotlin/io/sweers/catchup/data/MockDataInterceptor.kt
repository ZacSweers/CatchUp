/*
 * Copyright (c) 2018 Zac Sweers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.sweers.catchup.data

import android.content.Context
import io.sweers.catchup.P
import io.sweers.catchup.data.model.ServiceData
import io.sweers.catchup.util.injection.qualifiers.ApplicationContext
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody
import okio.buffer
import okio.source

/**
 * An interceptor that rewrites the response with mocked data instead.
 *
 * Note: This is pretty unmaintained right now.
 */
class MockDataInterceptor(@ApplicationContext private val context: Context) : Interceptor {

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
              context.assets
                  .open(formatUrl(serviceData, url)).source().buffer()
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
    // Can also use arrayMapOf()
    private val SUPPORTED_ENDPOINTS = mapOf<String, ServiceData>(
//        put(RedditApi.HOST,
//            ServiceData.Builder("r").addEndpoint("/")
//                .build())
//        put(MediumApi.HOST,
//            ServiceData.Builder("m").addEndpoint("/browse/top")
//                .build())
//        put(ProductHuntApi.HOST,
//            ServiceData.Builder("ph").addEndpoint("/v1/posts")
//                .build())
//        put(SlashdotApi.HOST,
//            ServiceData.Builder("sd").addEndpoint("/Slashdot/slashdotMainatom")
//                .fileType("xml")
//                .build())
//        put(DesignerNewsApi.HOST,
//            ServiceData.Builder("dn").addEndpoint("/api/v1/stories")
//                .build())
//        put(DribbbleApi.HOST,
//            ServiceData.Builder("dr").addEndpoint("/v1/shots")
//                .build())
    )

    private fun formatUrl(service: ServiceData, url: HttpUrl): String {
      var lastSegment = url.pathSegments()[url.pathSize() - 1]
      if ("" == lastSegment) {
        lastSegment = "nopath"
      }
      return service.assetsPrefix + "/" + lastSegment + "." + service.fileType
    }
  }
}
