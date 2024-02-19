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
