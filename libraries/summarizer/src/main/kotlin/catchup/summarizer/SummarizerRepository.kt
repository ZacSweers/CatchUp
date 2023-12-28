package catchup.summarizer

import catchup.di.AppScope
import catchup.di.FakeMode
import catchup.di.SingleIn
import catchup.summarizer.SummarizerResult.Error
import catchup.summarizer.SummarizerResult.NotFound
import catchup.summarizer.SummarizerResult.Success
import com.squareup.anvil.annotations.ContributesBinding
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import retrofit2.HttpException

interface SummarizerRepository {
  suspend fun getSummarization(url: String): SummarizerResult
}

sealed interface SummarizerResult {
  data class Success(val summary: String) : SummarizerResult

  data object NotFound : SummarizerResult

  data object Unavailable : SummarizerResult

  data class Error(val message: String) : SummarizerResult
}

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class SummarizerRepositoryImpl
@Inject
constructor(
  private val database: SummarizationsDatabase,
  private val chatGptApi: ChatGptApi,
  @FakeMode private val isFakeMode: StateFlow<Boolean>,
) : SummarizerRepository {
  override suspend fun getSummarization(url: String): SummarizerResult {
    if (isFakeMode.value) {
      return NotFound
    }
    return withContext(IO) {
      val summary =
        database.summarizationsQueries.transactionWithResult {
          database.summarizationsQueries.getSummarization(url).executeAsOneOrNull()?.summary
        }
      if (summary == NOT_FOUND) {
        return@withContext NotFound
      } else if (summary != null) {
        return@withContext Success(summary)
      }

      // Not stored, fetch it
      val response =
        try {
          chatGptApi.summarize(url)
        } catch (e: HttpException) {
          return@withContext Error(e.response()?.errorBody()?.string() ?: e.message())
        }

      val text = response.choices.first().message.content.trim()
      database.summarizationsQueries.transaction {
        database.summarizationsQueries.insert(url, text)
      }
      return@withContext Success(text)
    }
  }

  private companion object {
    const val NOT_FOUND = "CATCHUP_SUMMARIZER_NOT_FOUND"
  }
}
