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
