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

import okhttp3.Request
import okhttp3.ResponseBody
import okio.Buffer
import retrofit2.Call
import retrofit2.CallAdapter
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import java.io.EOFException
import java.io.IOException
import java.lang.reflect.Type
import java.nio.charset.Charset
import java.nio.charset.UnsupportedCharsetException

/**
 * Adapted from https://gist.github.com/NightlyNexus/2e880c86668815690cba8b61501c9e14
 */
class LoggingCallAdapterFactory(private val logger: Logger) : CallAdapter.Factory() {
  interface Logger {
    fun <T> onResponse(call: Call<T>, response: Response<T>) {

    }

    fun <T> onFailure(call: Call<T>, t: Throwable) {

    }
  }

  override fun get(returnType: Type,
      annotations: Array<Annotation>,
      retrofit: Retrofit): CallAdapter<*, *> {
    val adapter = retrofit.nextCallAdapter(this, returnType, annotations)
    val possiblyForwardingLogger = retrofit.callbackExecutor()?.let { executor ->
      object : Logger {
        override fun <T> onResponse(call: Call<T>, response: Response<T>) {
          executor.execute { logger.onResponse(call, response) }
        }

        override fun <T> onFailure(call: Call<T>, t: Throwable) {
          executor.execute { logger.onFailure(call, t) }
        }
      }
    } ?: logger
    return Adapter(possiblyForwardingLogger, adapter)
  }

  private class Adapter<R, T> internal constructor(private val logger: Logger,
      private val delegate: CallAdapter<R, T>) : CallAdapter<R, T> {

    override fun responseType(): Type {
      return delegate.responseType()
    }

    override fun adapt(call: Call<R>): T {
      return delegate.adapt(LoggingCall(logger, call))
    }
  }

  private class LoggingCall<R> internal constructor(internal val logger: Logger,
      private val delegate: Call<R>) : Call<R> {

    internal fun logResponse(response: Response<R>) {
      if (response.isSuccessful) {
        logger.onResponse(this, response)
      } else {
        val buffer = response.errorBody()!!.source().buffer()
        val bufferByteCount = buffer.size()
        logger.onResponse(this, response)
        if (bufferByteCount != buffer.size()) {
          throw IllegalStateException("Do not consume the error body. Bytes before: "
              + bufferByteCount
              + ". Bytes after: "
              + buffer.size()
              + ".")
        }
      }
    }

    override fun enqueue(callback: Callback<R>) {
      delegate.enqueue(object : Callback<R> {
        override fun onResponse(call: Call<R>, response: Response<R>) {
          logResponse(response)
          callback.onResponse(call, response)
        }

        override fun onFailure(call: Call<R>, t: Throwable) {
          logger.onFailure(call, t)
          callback.onFailure(call, t)
        }
      })
    }

    override fun isExecuted(): Boolean {
      return delegate.isExecuted
    }

    @Throws(IOException::class)
    override fun execute(): Response<R> {
      return try {
        val response = delegate.execute()
        logResponse(response)
        response
      } catch (e: IOException) {
        logger.onFailure(this, e)
        throw e
      }

    }

    override fun cancel() {
      delegate.cancel()
    }

    override fun isCanceled(): Boolean {
      return delegate.isCanceled
    }

    override // Performing deep clone.
    fun clone(): Call<R> {
      return LoggingCall(logger, delegate.clone())
    }

    override fun request(): Request {
      return delegate.request()
    }
  }
}

interface Analytics {
  fun httpFailure(statusCode: Int, errorMessage: String, url: String, method: String?)
  fun networkFailure(errorMessage: String?, url: String, method: String?)
}

class AnalyticsNetworkLogger(private val analytics: Analytics) : LoggingCallAdapterFactory.Logger {

  override fun <T> onResponse(call: Call<T>, response: Response<T>) {
    if (response.isSuccessful) {
      return
    }
    val request = response.raw().request()
    val errorMessage = errorMessage(response.errorBody())
    analytics.httpFailure(400, errorMessage, request.url().toString(), request.method())
  }

  override fun <T> onFailure(call: Call<T>, t: Throwable) {
    if (call.isCanceled) {
      return
    }
    val request = call.request()
    analytics.networkFailure(t.message, request.url().toString(), request.method())
  }

  companion object {

    internal fun errorMessage(errorBody: ResponseBody?): String {
      if (errorBody == null) {
        return ""
      }
      if (errorBody.contentLength() == 0L) {
        return ""
      }
      val charset: Charset
      val contentType = errorBody.contentType()
      charset = if (contentType == null) {
        Charsets.UTF_8
      } else {
        try {
          contentType.charset(Charsets.UTF_8)!!
        } catch (e: UnsupportedCharsetException) {
          // Charset is likely malformed.
          return "Unsupported Content-Type: " + contentType
        }

      }
      val source = errorBody.source()
      try {
        source.request(java.lang.Long.MAX_VALUE) // Buffer the entire body.
      } catch (e: IOException) {
        return "Error reading error body: " + e.message
      }

      val buffer = source.buffer()
      if (!isPlaintext(buffer)) {
        return "Error body is not plain text."
      }
      return buffer.clone().readString(charset)
    }

    /**
     * Returns true if the body in question probably contains human readable text. Uses a small sample
     * of code points to detect unicode control characters commonly used in binary file signatures.
     */
    private fun isPlaintext(buffer: Buffer): Boolean {
      try {
        val prefix = Buffer()
        val byteCount = (if (buffer.size() < 64) buffer.size() else 64).toLong()
        buffer.copyTo(prefix, 0, byteCount)
        for (i in 0..15) {
          if (prefix.exhausted()) {
            break
          }
          val codePoint = prefix.readUtf8CodePoint()
          if (Character.isISOControl(codePoint) && !Character.isWhitespace(codePoint)) {
            return false
          }
        }
        return true
      } catch (e: EOFException) {
        return false // Truncated UTF-8 sequence.
      }

    }
  }
}
