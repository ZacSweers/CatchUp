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

package io.sweers.catchup.analytics

import com.google.firebase.perf.FirebasePerformance
import io.reactivex.Completable
import io.reactivex.Maybe
import io.sweers.catchup.util.d
import java.util.concurrent.atomic.AtomicLong

/*
 * Utilities for tracing.
 */

fun <T> Maybe<T>.trace(tag: String): Maybe<T> {
  val trace = FirebasePerformance.getInstance().newTrace(tag)
  val timer = AtomicLong()
  return this
      .doOnSubscribe {
        trace.start()
        timer.set(System.currentTimeMillis())
      }
      .doOnSuccess {
        trace.incrementCounter("Success")
      }
      .doOnComplete {
        trace.incrementCounter("Empty")
      }
      .doOnError {
        trace.incrementCounter("Error")
      }
      .doFinally {
        trace.stop()
        d { "Stopped trace. $tag - took: ${System.currentTimeMillis() - timer.get()}ms" }
      }
}

fun Completable.trace(tag: String): Completable {
  val trace = FirebasePerformance.getInstance().newTrace(tag)
  val timer = AtomicLong()
  return this
      .doOnSubscribe {
        trace.start()
        timer.set(System.currentTimeMillis())
      }
      .doOnComplete {
        trace.incrementCounter("Success")
      }
      .doOnError {
        trace.incrementCounter("Error")
      }
      .doFinally {
        trace.stop()
        d { "Stopped trace. $tag - took: ${System.currentTimeMillis() - timer.get()}ms" }
      }
}
