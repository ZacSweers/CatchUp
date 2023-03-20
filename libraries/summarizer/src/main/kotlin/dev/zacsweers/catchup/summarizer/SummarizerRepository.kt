package dev.zacsweers.catchup.summarizer

import com.squareup.anvil.annotations.ContributesBinding
import dev.zacsweers.catchup.di.AppScope
import dev.zacsweers.catchup.di.SingleIn
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import retrofit2.HttpException

interface SummarizerRepository {
  suspend fun getSummarization(url: String): SummarizerResult
}

sealed interface SummarizerResult {
  data class Success(val summary: String) : SummarizerResult
  object NotFound : SummarizerResult
  data class Error(val message: String) : SummarizerResult
}

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class SummarizerRepositoryImpl
@Inject
constructor(private val database: SummarizationsDatabase, private val chatGptApi: ChatGptApi) :
  SummarizerRepository {
  override suspend fun getSummarization(url: String): SummarizerResult =
    withContext(IO) {
      val summary =
        database.summarizationsQueries.transactionWithResult {
          database.summarizationsQueries.getSummarization(url).executeAsOneOrNull()?.summary
        }
      if (summary == NOT_FOUND) {
        return@withContext SummarizerResult.NotFound
      } else if (summary != null) {
        return@withContext SummarizerResult.Success(summary)
      }

      // Not stored, fetch it
      val response =
        try {
          chatGptApi.summarize(url)
        } catch (e: HttpException) {
          return@withContext SummarizerResult.Error(
            e.response()?.errorBody()?.string() ?: e.message()
          )
        }

      val text = response.choices.first().message.content.trim()
      database.summarizationsQueries.transaction {
        database.summarizationsQueries.insert(url, text)
      }
      return@withContext SummarizerResult.Success(text)
    }

  private companion object {
    const val NOT_FOUND = "CATCHUP_SUMMARIZER_NOT_FOUND"
  }
}
