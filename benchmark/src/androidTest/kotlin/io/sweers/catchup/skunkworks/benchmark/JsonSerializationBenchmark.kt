/*
 * Copyright (c) 2019 Zac Sweers
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

package io.sweers.catchup.skunkworks.benchmark

import android.content.Context
import androidx.benchmark.BenchmarkRule
import androidx.benchmark.measureRepeated
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.base.Charsets
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.sweers.catchup.skunkworks.benchmark.adapter.GeneratedJsonAdapterFactory
import io.sweers.catchup.skunkworks.benchmark.adapter.GeneratedTypeAdapterFactory
import io.sweers.catchup.skunkworks.benchmark.kotlinx_serialization.Response
import io.sweers.catchup.skunkworks.benchmark.model_av.ResponseAV
import io.sweers.catchup.skunkworks.benchmark.moshiKotlinCodegen.KCGResponse
import io.sweers.catchup.skunkworks.benchmark.moshiKotlinReflective.KRResponse
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.KSerializer
import okio.Buffer
import okio.BufferedSink
import okio.BufferedSource
import okio.buffer
import okio.source
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.Reader
import java.io.Writer

@ImplicitReflectionSerializer
@LargeTest
@RunWith(AndroidJUnit4::class)
class JsonSerializationBenchmark {

  class KotlinxSerialization {

    val json: String
    val minifiedJson: String
    val response: Response
    val kSerializer: KSerializer<Response>

    init {
      val url = ApplicationProvider.getApplicationContext<Context>().assets.open("largesample.json")
      json = url.source().buffer().readUtf8()
      val url2 = ApplicationProvider.getApplicationContext<Context>().assets.open(
          "largesample_minified.json")
      minifiedJson = url2.source().buffer().readUtf8()
      kSerializer = Response.underlyingSerializer()
      response = Response.parse(kSerializer, json)
    }
  }

  class ReflectiveMoshi {

    private val moshi: Moshi = Moshi.Builder().build()
    val json: String
    val response: Response
    val adapter: JsonAdapter<Response>

    init {
      val url = ApplicationProvider.getApplicationContext<Context>().assets.open("largesample.json")
      json = url.source().buffer().readUtf8()
      adapter = moshi.adapter(Response::class.java)
      response = adapter.fromJson(json)!!
    }
  }

  class ReflectiveGson {

    private val gson: Gson = GsonBuilder().create()
    val json: String
    val response: Response
    val adapter: TypeAdapter<Response>

    init {
      val url = ApplicationProvider.getApplicationContext<Context>().assets.open("largesample.json")
      json = url.source().buffer().readUtf8()
      adapter = gson.getAdapter(Response::class.java)
      response = adapter.fromJson(json)
    }
  }

  class AVMoshi {

    private val moshi: Moshi = Moshi.Builder()
        .add(GeneratedJsonAdapterFactory.create())
        .build()
    val json: String
    val minifiedJson: String
    val response: ResponseAV
    val adapter: JsonAdapter<ResponseAV>

    init {
      val url = ApplicationProvider.getApplicationContext<Context>().assets.open("largesample.json")
      json = url.source().buffer().readUtf8()
      val url2 = ApplicationProvider.getApplicationContext<Context>().assets.open(
          "largesample_minified.json")
      minifiedJson = url2.source().buffer().readUtf8()
      adapter = moshi.adapter(ResponseAV::class.java)
      response = adapter.fromJson(json)!!
    }
  }

  class ReflectiveMoshiKotlin {

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    val json: String
    private val minifiedJson: String
    val response: KRResponse
    val adapter: JsonAdapter<KRResponse>

    init {
      val url = ApplicationProvider.getApplicationContext<Context>().assets.open("largesample.json")
      json = url.source().buffer().readUtf8()
      val url2 = ApplicationProvider.getApplicationContext<Context>().assets.open(
          "largesample_minified.json")
      minifiedJson = url2.source().buffer().readUtf8()
      adapter = moshi.adapter(KRResponse::class.java)
      response = adapter.fromJson(json)!!
    }
  }

  class CodegenMoshiKotlin {

    private val moshi: Moshi = Moshi.Builder()
        .build()
    val json: String
    val minifiedJson: String
    val response: KCGResponse
    val adapter: JsonAdapter<KCGResponse>

    init {
      val url = ApplicationProvider.getApplicationContext<Context>().assets.open("largesample.json")
      json = url.source().buffer().readUtf8()
      val url2 = ApplicationProvider.getApplicationContext<Context>().assets.open(
          "largesample_minified.json")
      minifiedJson = url2.source().buffer().readUtf8()
      adapter = moshi.adapter(KCGResponse::class.java)
      response = adapter.fromJson(json)!!
    }
  }

  class AVMoshiBuffer {

    private val moshi: Moshi = Moshi.Builder()
        .add(GeneratedJsonAdapterFactory.create())
        .build()
    private val json: String
    private val minifiedJson: String
    lateinit var bufferedSource: BufferedSource
    lateinit var minifiedBufferedSource: BufferedSource
    lateinit var bufferedSink: BufferedSink
    val response: ResponseAV
    val adapter: JsonAdapter<ResponseAV>

    init {
      val url = ApplicationProvider.getApplicationContext<Context>().assets.open("largesample.json")
      json = url.source().buffer().readUtf8()
      val url2 = ApplicationProvider.getApplicationContext<Context>().assets.open(
          "largesample_minified.json")
      minifiedJson = url2.source().buffer().readUtf8()
      adapter = moshi.adapter(ResponseAV::class.java)
      response = adapter.fromJson(json)!!
    }

    fun setupIteration() {
      bufferedSource = Buffer().write(json.toByteArray())
      minifiedBufferedSource = Buffer().write(json.toByteArray())
      bufferedSink = Buffer()
    }
  }

  class ReflectiveMoshiKotlinBuffer {

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    private val json: String
    private val minifiedJson: String
    lateinit var bufferedSource: BufferedSource
    lateinit var minifiedBufferedSource: BufferedSource
    lateinit var bufferedSink: BufferedSink
    val response: KRResponse
    val adapter: JsonAdapter<KRResponse>

    init {
      val url = ApplicationProvider.getApplicationContext<Context>().assets.open("largesample.json")
      json = url.source().buffer().readUtf8()
      val url2 = ApplicationProvider.getApplicationContext<Context>().assets.open(
          "largesample_minified.json")
      minifiedJson = url2.source().buffer().readUtf8()
      adapter = moshi.adapter(KRResponse::class.java)
      response = adapter.fromJson(json)!!
    }

    fun setupIteration() {
      bufferedSource = Buffer().write(json.toByteArray())
      minifiedBufferedSource = Buffer().write(minifiedJson.toByteArray())
      bufferedSink = Buffer()
    }
  }

  class CodegenMoshiKotlinBuffer {

    private val moshi: Moshi = Moshi.Builder()
        .build()
    private val json: String
    private val minifiedJson: String
    lateinit var bufferedSource: BufferedSource
    lateinit var minifiedBufferedSource: BufferedSource
    lateinit var bufferedSink: BufferedSink
    val response: KCGResponse
    val adapter: JsonAdapter<KCGResponse>

    init {
      val url = ApplicationProvider.getApplicationContext<Context>().assets.open("largesample.json")
      json = url.source().buffer().readUtf8()
      val url2 = ApplicationProvider.getApplicationContext<Context>().assets.open(
          "largesample_minified.json")
      minifiedJson = url2.source().buffer().readUtf8()
      adapter = moshi.adapter(KCGResponse::class.java)
      response = adapter.fromJson(json)!!
    }

    fun setupIteration() {
      bufferedSource = Buffer().write(json.toByteArray())
      minifiedBufferedSource = Buffer().write(minifiedJson.toByteArray())
      bufferedSink = Buffer()
    }
  }

  class AVGson {

    private val gson: Gson = GsonBuilder()
        .registerTypeAdapterFactory(GeneratedTypeAdapterFactory.create())
        .create()
    val json: String
    val minifiedJson: String
    val response: ResponseAV
    val adapter: TypeAdapter<ResponseAV>

    init {
      val url = ApplicationProvider.getApplicationContext<Context>().assets.open("largesample.json")
      json = url.source().buffer().readUtf8()
      val url2 = ApplicationProvider.getApplicationContext<Context>().assets.open(
          "largesample_minified.json")
      minifiedJson = url2.source().buffer().readUtf8()
      adapter = gson.getAdapter(ResponseAV::class.java)
      response = adapter.fromJson(json)
    }
  }

  class AVGsonBuffer {

    private val gson: Gson = GsonBuilder()
        .registerTypeAdapterFactory(GeneratedTypeAdapterFactory.create())
        .create()
    private val json: String
    private val minifiedJson: String
    lateinit var source: Reader
    lateinit var sink: Writer
    lateinit var minifiedSource: Reader
    val response: ResponseAV
    val adapter: TypeAdapter<ResponseAV>

    init {
      val url = ApplicationProvider.getApplicationContext<Context>().assets.open("largesample.json")
      json = url.source().buffer().readUtf8()
      val url2 = ApplicationProvider.getApplicationContext<Context>().assets.open(
          "largesample_minified.json")
      minifiedJson = url2.source().buffer().readUtf8()
      adapter = gson.getAdapter(ResponseAV::class.java)
      response = adapter.fromJson(json)
    }

    fun setupIteration() {
      source = InputStreamReader(Buffer().write(json.toByteArray()).inputStream(), Charsets.UTF_8)
      sink = OutputStreamWriter(Buffer().outputStream(), Charsets.UTF_8)
      minifiedSource = InputStreamReader(Buffer().write(minifiedJson.toByteArray()).inputStream(),
          Charsets.UTF_8)
    }
  }

  @get:Rule
  val benchmarkRule = BenchmarkRule()

  @Test
  fun kserializer_string_toJson() = benchmarkRule.measureRepeated {
    val param = runWithTimingDisabled { KotlinxSerialization() }
    param.response.stringify(param.kSerializer)
  }

  @Test
  fun moshi_reflective_string_toJson() = benchmarkRule.measureRepeated {
    val param = runWithTimingDisabled { ReflectiveMoshi() }
    param.adapter.toJson(param.response)
  }

  @Test
  fun moshi_autovalue_string_toJson() = benchmarkRule.measureRepeated {
    val param = runWithTimingDisabled { AVMoshi() }
    param.adapter.toJson(param.response)
  }

  @Test
  fun moshi_kotlin_reflective_string_toJson() = benchmarkRule.measureRepeated {
    val param = runWithTimingDisabled { ReflectiveMoshiKotlin() }
    param.adapter.toJson(param.response)
  }

  @Test
  fun moshi_kotlin_codegen_string_toJson() = benchmarkRule.measureRepeated {
    val param = runWithTimingDisabled { CodegenMoshiKotlin() }
    param.adapter.toJson(param.response)
  }

  @Test
  fun moshi_autovalue_buffer_toJson() = benchmarkRule.measureRepeated {
    val param = runWithTimingDisabled { AVMoshiBuffer().also { it.setupIteration() } }
    param.adapter.toJson(param.bufferedSink, param.response)
  }

  @Test
  fun moshi_kotlin_reflective_buffer_toJson() = benchmarkRule.measureRepeated {
    val param = runWithTimingDisabled { ReflectiveMoshiKotlinBuffer().also { it.setupIteration() } }
    param.adapter.toJson(param.bufferedSink, param.response)
  }

  @Test
  fun moshi_kotlin_codegen_buffer_toJson() = benchmarkRule.measureRepeated {
    val param = runWithTimingDisabled { CodegenMoshiKotlinBuffer().also { it.setupIteration() } }
    param.adapter.toJson(param.bufferedSink, param.response)
  }

  @Test
  fun gson_reflective_string_toJson() = benchmarkRule.measureRepeated {
    val param = runWithTimingDisabled { ReflectiveGson() }
    param.adapter.toJson(param.response)
  }

  @Test
  fun gson_autovalue_string_toJson() = benchmarkRule.measureRepeated {
    val param = runWithTimingDisabled { AVGson() }
    param.adapter.toJson(param.response)
  }

  @Test
  fun gson_autovalue_buffer_toJson() = benchmarkRule.measureRepeated {
    val param = runWithTimingDisabled { AVGsonBuffer().also { it.setupIteration() } }
    param.adapter.toJson(param.sink, param.response)
  }

  @Test
  fun kserializer_string_fromJson() = benchmarkRule.measureRepeated {
    val param = runWithTimingDisabled { KotlinxSerialization() }
    Response.parse(param.kSerializer, param.json)
  }

  @Test
  fun kserializer_string_fromJson_minified() = benchmarkRule.measureRepeated {
    val param = runWithTimingDisabled { KotlinxSerialization() }
    Response.parse(param.kSerializer, param.minifiedJson)
  }

  @Test
  fun moshi_reflective_string_fromJson() = benchmarkRule.measureRepeated {
    val param = runWithTimingDisabled { ReflectiveMoshi() }
    param.adapter.fromJson(param.json)
  }

  @Test
  fun moshi_autovalue_string_fromJson() = benchmarkRule.measureRepeated {
    val param = runWithTimingDisabled { AVMoshi() }
    param.adapter.fromJson(param.json)
  }

  @Test
  fun moshi_autovalue_string_fromJson_minified() = benchmarkRule.measureRepeated {
    val param = runWithTimingDisabled { AVMoshi() }
    param.adapter.fromJson(param.minifiedJson)
  }

  @Test
  fun moshi_kotlin_reflective_string_fromJson() = benchmarkRule.measureRepeated {
    val param = runWithTimingDisabled { ReflectiveMoshiKotlin() }
    param.adapter.fromJson(param.json)
  }

  @Test
  fun moshi_kotlin_codegen_string_fromJson() = benchmarkRule.measureRepeated {
    val param = runWithTimingDisabled { CodegenMoshiKotlin() }
    param.adapter.fromJson(param.json)
  }

  @Test
  fun moshi_kotlin_codegen_string_fromJson_minified() = benchmarkRule.measureRepeated {
    val param = runWithTimingDisabled { CodegenMoshiKotlin() }
    param.adapter.fromJson(param.minifiedJson)
  }

  @Test
  fun moshi_autovalue_buffer_fromJson() = benchmarkRule.measureRepeated {
    val param = runWithTimingDisabled { AVMoshiBuffer().also { it.setupIteration() } }
    param.adapter.fromJson(param.bufferedSource)
  }

  @Test
  fun moshi_autovalue_buffer_fromJson_minified() = benchmarkRule.measureRepeated {
    val param = runWithTimingDisabled { AVMoshiBuffer().also { it.setupIteration() } }
    param.adapter.fromJson(param.minifiedBufferedSource)
  }

  @Test
  fun moshi_kotlin_reflective_buffer_fromJson() = benchmarkRule.measureRepeated {
    val param = runWithTimingDisabled { ReflectiveMoshiKotlinBuffer().also { it.setupIteration() } }
    param.adapter.fromJson(param.bufferedSource)
  }

  @Test
  fun moshi_kotlin_reflective_buffer_fromJson_minified() = benchmarkRule.measureRepeated {
    val param = runWithTimingDisabled { ReflectiveMoshiKotlinBuffer().also { it.setupIteration() } }
    param.adapter.fromJson(param.minifiedBufferedSource)
  }

  @Test
  fun moshi_kotlin_codegen_buffer_fromJson() = benchmarkRule.measureRepeated {
    val param = runWithTimingDisabled { CodegenMoshiKotlinBuffer().also { it.setupIteration() } }
    param.adapter.fromJson(param.bufferedSource)
  }

  @Test
  fun moshi_kotlin_codegen_buffer_fromJson_minified() = benchmarkRule.measureRepeated {
    val param = runWithTimingDisabled { CodegenMoshiKotlinBuffer().also { it.setupIteration() } }
    param.adapter.fromJson(param.minifiedBufferedSource)
  }

  @Test
  fun gson_reflective_string_fromJson() = benchmarkRule.measureRepeated {
    val param = runWithTimingDisabled { ReflectiveGson() }
    param.adapter.fromJson(param.json)
  }

  @Test
  fun gson_autovalue_string_fromJson() = benchmarkRule.measureRepeated {
    val param = runWithTimingDisabled { AVGson() }
    param.adapter.fromJson(param.json)
  }

  @Test
  fun gson_autovalue_string_fromJson_minified() = benchmarkRule.measureRepeated {
    val param = runWithTimingDisabled { AVGson() }
    param.adapter.fromJson(param.minifiedJson)
  }

  @Test
  fun gson_autovalue_buffer_fromJson() = benchmarkRule.measureRepeated {
    val param = runWithTimingDisabled { AVGsonBuffer().also { it.setupIteration() } }
    param.adapter.fromJson(param.source)
  }

  @Test
  fun gson_autovalue_buffer_fromJson_minified() = benchmarkRule.measureRepeated {
    val param = runWithTimingDisabled { AVGsonBuffer().also { it.setupIteration() } }
    param.adapter.fromJson(param.minifiedSource)
  }
}
