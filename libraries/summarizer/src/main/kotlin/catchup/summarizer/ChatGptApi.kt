/*
 * Copyright (C) 2026. Zac Sweers
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
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
package catchup.summarizer

import androidx.annotation.Keep
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.POST

@Keep // https://issuetracker.google.com/issues/271675881
interface ChatGptApi {
  @POST("/v1/chat/completions")
  suspend fun completion(@Body request: CompletionRequest): CompletionResponse

  suspend fun summarize(url: String) =
    completion(CompletionRequest(messages = listOf(Message(content = "Summarize this $url"))))
}

@JsonClass(generateAdapter = true)
data class CompletionRequest(val model: String = "gpt-3.5-turbo", val messages: List<Message>)

@JsonClass(generateAdapter = true)
data class Message(val role: String = "user", val content: String)

@JsonClass(generateAdapter = true)
data class CompletionResponse(
  val id: String,
  @Json(name = "object") val objectType: String,
  val created: Long,
  val model: String,
  val choices: List<Choice>,
  val usage: Usage,
) {

  @JsonClass(generateAdapter = true)
  data class Choice(
    val index: Int,
    @Json(name = "finish_reason") val finishReason: String,
    val message: Message,
  )

  @JsonClass(generateAdapter = true)
  data class Usage(
    @Json(name = "prompt_tokens") val promptTokens: Int,
    @Json(name = "completion_tokens") val completionTokens: Int,
    @Json(name = "total_tokens") val totalTokens: Int,
  )
}
