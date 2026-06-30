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

import catchup.di.FakeMode
import catchup.summarizer.SummarizerResult.Error
import catchup.summarizer.SummarizerResult.NotFound
import catchup.summarizer.SummarizerResult.Success
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
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
@Inject
class SummarizerRepositoryImpl(
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
