package io.sweers.catchup.analytics

import android.app.Activity
import android.support.v4.app.FrameMetricsAggregator
import com.google.firebase.perf.FirebasePerformance
import io.reactivex.Completable
import io.reactivex.Maybe
import io.sweers.catchup.util.d
import io.sweers.catchup.util.isN
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.atomic.AtomicLong

/*
 * Utilities for tracing.
 */

inline fun <T> Maybe<T>.trace(tag: String): Maybe<T> {
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

inline fun Completable.trace(tag: String): Completable {
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

fun traceFrameMetrics(activity: Activity?, tag: String, body: () -> Unit) {
  if (activity == null) {
    throw IllegalStateException("No activity!")
  }
  if (isN()) {
    val trace = FirebasePerformance.getInstance().newTrace(tag)
    val aggregator = FrameMetricsAggregator(FrameMetricsAggregator.TOTAL_DURATION)
        .apply {
          add(activity)
        }
    body()
    Completable.complete()
        .delay(2, SECONDS)
        .blockingAwait()
    aggregator.stop()?.let { metrics ->
      val frames = metrics[FrameMetricsAggregator.TOTAL_INDEX]
      (0..frames.size())
          .map { frames[it] }
          .filter { it > 16 }
          .forEach {
            d { "Slow frame found! ${it}ms" }
            if (it > 700) {
              trace.incrementCounter("${tag}_frozen_frames", it.toLong())
            }
            trace.incrementCounter("${tag}_slow_frames", it.toLong())
          }
    }
    trace.stop()
  } else {
    body()
  }
}
