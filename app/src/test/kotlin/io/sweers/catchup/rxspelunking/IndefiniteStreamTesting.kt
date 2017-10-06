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

package io.sweers.catchup.rxspelunking

import io.reactivex.Emitter
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import org.junit.Test
import java.util.concurrent.TimeUnit

private fun <T> Subject<T>.sequenceSelf(
    requestNext: (T) -> Maybe<T>,
    nextHandler: (T, Emitter<T>) -> Unit): Observable<T> {
  return flatMapMaybe(requestNext)
      .doAfterNext { result ->
        nextHandler(result, object : Emitter<T> {
          override fun onComplete() {
            this@sequenceSelf.onComplete()
          }

          override fun onNext(value: T) {
            this@sequenceSelf.onNext(value)
          }

          override fun onError(error: Throwable) {
            this@sequenceSelf.onError(error)
          }
        })
      }
}

class GenerateTest {

  data class Result(val data: List<String>, var nextPageToken: String?)

  @Test
  fun testDataResultsSeq() {
    val targetPage = "5"
    BehaviorSubject.createDefault(Result(emptyList(), "0")).toSerialized()
        .sequenceSelf({ getPage(it.nextPageToken!!) }) { result, emitter ->
          val nextPage = result.nextPageToken
          if (nextPage != null) {
            // Always request the next page if it's not null. If it's the last page, we'll complete after this
            emitter.onNext(result)
          }
          if (nextPage == null || nextPage == targetPage) {
            // Complete if there's no more pages or we've hit the target page
            emitter.onComplete()
          }
        }
        .reduce { prev, result ->
          Result(prev.data + result.data, result.nextPageToken)
        }
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(Result(listOf("0", "1", "2", "3", "4", "5"), "6"))
  }

  @Test
  fun testDataResults() {
    val targetPage = "5"
    val stateHandler = BehaviorSubject.createDefault(Result(emptyList(), "0")).toSerialized()
    stateHandler
        .flatMapMaybe { getPage(it.nextPageToken!!) }
        .doAfterNext { result ->
          val nextPage = result.nextPageToken
          if (nextPage != null) {
            // Always request the next page if it's not null. If it's the last page, we'll complete after this
            stateHandler.onNext(result)
          }
          if (nextPage == null || nextPage == targetPage) {
            // Complete if there's no more pages or we've hit the target page
            stateHandler.onComplete()
          }
        }
        .reduce { prev, result ->
          Result(prev.data + result.data, result.nextPageToken)
        }
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(Result(listOf("0", "1", "2", "3", "4", "5"), "6"))
  }

  private fun getPage(page: String): Maybe<Result> = when (page) {
    "0" -> Maybe.just(Result(listOf("0"), "1"))
    "1" -> Maybe.just(Result(listOf("1"), "2"))
    "2" -> Maybe.just(Result(listOf("2"), "3"))
    "3" -> Maybe.just(Result(listOf("3"), "4"))
    "4" -> Maybe.just(Result(listOf("4"), "5"))
    "5" -> Maybe.just(Result(listOf("5"), "6"))
    else -> throw UnsupportedOperationException("Page was $page")
  }

}
