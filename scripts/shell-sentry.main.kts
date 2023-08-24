#!/usr/bin/env kotlin
@file:DependsOn("com.github.ajalt.clikt:clikt-jvm:4.2.0")
@file:DependsOn("com.slack.cli:kotlin-cli-util:2.2.0")
@file:DependsOn("com.squareup.retrofit2:retrofit:2.9.0")
@file:DependsOn("com.squareup.retrofit2:converter-moshi:2.9.0")
@file:DependsOn("com.squareup.okhttp3:okhttp:5.0.0-alpha.11")
@file:DependsOn("com.squareup.moshi:moshi:1.15.0")
@file:DependsOn("com.squareup.moshi:moshi-kotlin:1.15.0")

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.nio.file.Path
import kotlin.io.path.readText
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.create
import retrofit2.http.Body
import retrofit2.http.POST
import slack.cli.shellsentry.AnalysisResult
import slack.cli.shellsentry.NoStacktraceThrowable
import slack.cli.shellsentry.RetrySignal
import slack.cli.shellsentry.ShellSentry
import slack.cli.shellsentry.ShellSentryExtension

class GuessedIssue(message: String) : NoStacktraceThrowable(message)

class AiClient(private val accessToken: String) {

  private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
  private val client =
    OkHttpClient.Builder()
      .addInterceptor { chain ->
        val request =
          chain.request().newBuilder().addHeader("Authorization", "Bearer $accessToken").build()
        chain.proceed(request)
      }
      .build()
  private val api =
    Retrofit.Builder()
      .baseUrl("https://api.openai.com")
      .client(client)
      .addConverterFactory(MoshiConverterFactory.create(moshi))
      .build()
      .create<ChatGptApi>()

  suspend fun analyze(content: String): AnalysisResult? {
    return try {
      val contentTokens = content.split(" ").size
      val truncatedContent =
        if (contentTokens > ChatGptApi.remainingTokens) {
          println("Truncating content due to token limit")
          val tokensToRemove = contentTokens - ChatGptApi.remainingTokens
          val split = content.split(" ")
          // Split in reverse as we want to focus on the end of the content and travel up
          split.asReversed().subList(tokensToRemove, split.size).reversed().joinToString(" ")
        } else {
          content
        }
      val prompt = "${ChatGptApi.ANALYSIS_PROMPT}\n\n$truncatedContent"
      val response = api.analyze(prompt)
      val rawJson = response.choices.first().message.content.trim()
      val parsed =
        moshi.adapter(ChoiceAnalysis::class.java).fromJson(rawJson)
          ?: error("Could not parse: $rawJson")
      AnalysisResult(
        parsed.message,
        parsed.explanation,
        if (parsed.retry) RetrySignal.RetryImmediately else RetrySignal.Ack,
        parsed.confidence
      ) { message ->
        GuessedIssue(message)
      }
    } catch (t: Throwable) {
      t.printStackTrace()
      null
    }
  }

  interface ChatGptApi {
    @POST("/v1/chat/completions")
    suspend fun completion(@Body request: CompletionRequest): CompletionResponse

    suspend fun analyze(content: String): CompletionResponse {
      return completion(CompletionRequest(messages = listOf(Message(content = content))))
    }

    companion object {
      private const val MAX_TOKENS = 4096

      val ANALYSIS_PROMPT =
        """
          Given the following console output, please provide a diagnosis in a raw JSON object format:

          - "message": A broad single-line description of the error without specifying exact details, suitable for crash reporter grouping.
          - "explanation": A detailed, multi-line message explaining the error and suggesting a solution.
          - "retry": A boolean value (true/false) indicating whether a retry could potentially resolve the CI issue.
          - "confidence": An integer value between 1-100 representing your confidence about the accuracy of your error identification.
        """
          .trimIndent()

      private val promptTokens = ANALYSIS_PROMPT.split(" ").size
      val remainingTokens = MAX_TOKENS - promptTokens - 100 // 100 for buffer
    }
  }

  @JsonClass(generateAdapter = false)
  data class CompletionRequest(val model: String = "gpt-3.5-turbo", val messages: List<Message>)

  @JsonClass(generateAdapter = false)
  data class Message(val role: String = "user", val content: String)

  @JsonClass(generateAdapter = false)
  data class CompletionResponse(
    val id: String,
    @Json(name = "object") val objectType: String,
    val created: Long,
    val model: String,
    val choices: List<Choice>,
    val usage: Usage
  ) {

    @JsonClass(generateAdapter = false)
    data class Choice(
      val index: Int,
      @Json(name = "finish_reason") val finishReason: String,
      val message: Message,
    )

    @JsonClass(generateAdapter = false)
    data class Usage(
      @Json(name = "prompt_tokens") val promptTokens: Int,
      @Json(name = "completion_tokens") val completionTokens: Int,
      @Json(name = "total_tokens") val totalTokens: Int,
    )
  }

  @JsonClass(generateAdapter = false)
  data class ChoiceAnalysis(
    val message: String,
    val explanation: String,
    val retry: Boolean,
    val confidence: Int,
  )
}

class AiExtension(private val aiClient: AiClient) : ShellSentryExtension {
  override fun check(
    command: String,
    exitCode: Int,
    isAfterRetry: Boolean,
    consoleOutput: Path
  ): AnalysisResult? {
    return runBlocking { aiClient.analyze(consoleOutput.readText()) }
  }
}

val openAiKey: String? = System.getenv("OPEN_AI_KEY")
val extensions = if (openAiKey != null) listOf(AiExtension(AiClient(openAiKey))) else emptyList()

ShellSentry.create(args, ::println).copy(extensions = extensions).exec()
