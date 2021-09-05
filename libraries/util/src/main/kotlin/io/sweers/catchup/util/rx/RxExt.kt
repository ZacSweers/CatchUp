/*
 * Copyright (C) 2019. Zac Sweers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
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
package io.sweers.catchup.util.rx

import android.view.View
import com.google.android.material.snackbar.Snackbar
import io.reactivex.rxjava3.android.MainThreadDisposable.verifyMainThread
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.CompletableSource
import io.reactivex.rxjava3.core.CompletableTransformer
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.MaybeSource
import io.reactivex.rxjava3.core.MaybeTransformer
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.ObservableSource
import io.reactivex.rxjava3.core.ObservableTransformer
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.core.SingleSource
import io.reactivex.rxjava3.core.SingleTransformer
import java.util.concurrent.TimeUnit

@Suppress("UNCHECKED_CAST")
abstract class OmniTransformer<Upstream : Any, Downstream : Any> :
  ObservableTransformer<Upstream, Downstream>,
  SingleTransformer<Upstream, Downstream>,
  MaybeTransformer<Upstream, Downstream>,
  CompletableTransformer {

  override fun apply(upstream: Completable): CompletableSource = upstream

  override fun apply(upstream: Maybe<Upstream>) = upstream as MaybeSource<Downstream>

  override fun apply(upstream: Observable<Upstream>) = upstream as ObservableSource<Downstream>

  override fun apply(upstream: Single<Upstream>) = upstream as SingleSource<Downstream>
}

fun <T : Any> Observable<T>.doOnEmpty(action: () -> Unit): Observable<T> =
  switchIfEmpty(Observable.empty<T>().doOnComplete(action))

fun <T : Any> Observable<T>.delayedMessage(view: View, message: String): Observable<T> =
  compose(delayedMessageTransformer(view, message))

fun <T : Any> Single<T>.delayedMessage(view: View, message: String): Single<T> =
  compose(delayedMessageTransformer(view, message))

fun <T : Any> Maybe<T>.delayedMessage(view: View, message: String): Maybe<T> =
  compose(delayedMessageTransformer(view, message))

fun Completable.delayedMessage(view: View, message: String): Completable =
  compose(delayedMessageTransformer<Any>(view, message))

fun <T : Any> delayedMessageTransformer(view: View, message: String): OmniTransformer<T, T> {
  var snackbar: Snackbar? = null
  return timeoutActionTransformer(
    onTimeout = {
      verifyMainThread()
      snackbar = Snackbar.make(view, message, Snackbar.LENGTH_INDEFINITE).apply { show() }
    },
    onTerminate = { snackbar?.dismiss()?.also { snackbar = null } }
  )
}

fun <T : Any> Observable<T>.timeoutAction(
  onTimeout: (() -> Unit)? = null,
  onTerminate: (() -> Unit)? = null
): Observable<T> =
  compose(timeoutActionTransformer(onTimeout, onTerminate))

fun <T : Any> Single<T>.timeoutAction(
  onTimeout: (() -> Unit)? = null,
  onTerminate: (() -> Unit)? = null
): Single<T> =
  compose(timeoutActionTransformer(onTimeout, onTerminate))

fun <T : Any> Maybe<T>.timeoutAction(
  onTimeout: (() -> Unit)? = null,
  onTerminate: (() -> Unit)? = null
): Maybe<T> =
  compose(timeoutActionTransformer(onTimeout, onTerminate))

fun Completable.timeoutAction(
  onTimeout: (() -> Unit)? = null,
  onTerminate: (() -> Unit)? = null
): Completable =
  compose(timeoutActionTransformer<Any>(onTimeout, onTerminate))

fun <T : Any> timeoutActionTransformer(
  onTimeout: (() -> Unit)? = null,
  onTerminate: (() -> Unit)? = null,
  delay: Long = 300
): OmniTransformer<T, T> =
  object : OmniTransformer<T, T>() {

    private val timer = Observable.timer(delay, TimeUnit.MILLISECONDS)

    override fun apply(upstream: Observable<T>): Observable<T> {
      val shared = upstream.share()
      return shared
        .doOnSubscribe {
          timer.takeUntil(shared)
            .subscribe {
              onTimeout?.invoke()
            }
        }
        .doOnTerminate {
          onTerminate?.invoke()
        }
    }

    override fun apply(upstream: Maybe<T>) = apply(upstream.toObservable()).firstElement()

    override fun apply(upstream: Single<T>) = apply(upstream.toObservable()).firstOrError()

    override fun apply(upstream: Completable) = apply(upstream.toObservable()).ignoreElements()
  }

/**
 * Utility for working with enums when you want to run actions only on specific values
 */
inline fun <T : Enum<T>> Observable<T>.doOn(
  target: T,
  crossinline action: () -> Unit
): Observable<T> = apply {
  doOnNext { if (it == target) action() }
}

inline fun <reified R : Any> Observable<*>.filterIsInstance(): Observable<R> {
  return filter { it is R }.cast(R::class.java)
}

inline fun <reified R : Any> Flowable<*>.filterIsInstance(): Flowable<R> {
  return filter { it is R }.cast(R::class.java)
}

inline fun <reified R : Any> Single<*>.filterIsInstance(): Maybe<R> {
  return filter { it is R }.cast(R::class.java)
}

inline fun <reified R : Any> Maybe<*>.filterIsInstance(): Maybe<R> {
  return filter { it is R }.cast(R::class.java)
}
