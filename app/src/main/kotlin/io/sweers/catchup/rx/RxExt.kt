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

package io.sweers.catchup.rx

import android.support.design.widget.Snackbar
import android.view.View
import io.reactivex.Completable
import io.reactivex.CompletableSource
import io.reactivex.CompletableTransformer
import io.reactivex.Maybe
import io.reactivex.MaybeSource
import io.reactivex.MaybeTransformer
import io.reactivex.Observable
import io.reactivex.ObservableSource
import io.reactivex.ObservableTransformer
import io.reactivex.Single
import io.reactivex.SingleSource
import io.reactivex.SingleTransformer
import rx.android.MainThreadSubscription.verifyMainThread
import java.util.concurrent.TimeUnit

@Suppress("UNCHECKED_CAST")
abstract class OmniTransformer<Upstream, Downstream>
  : ObservableTransformer<Upstream, Downstream>,
    SingleTransformer<Upstream, Downstream>,
    MaybeTransformer<Upstream, Downstream>,
    CompletableTransformer {

  override fun apply(upstream: Completable): CompletableSource = upstream

  override fun apply(upstream: Maybe<Upstream>) = upstream as MaybeSource<Downstream>

  override fun apply(upstream: Observable<Upstream>) = upstream as ObservableSource<Downstream>

  override fun apply(upstream: Single<Upstream>) = upstream as SingleSource<Downstream>
}

fun <T> Observable<T>.doOnEmpty(action: () -> Unit): Observable<T> =
    switchIfEmpty(Observable.empty<T>().doOnComplete(action))

fun <T : Any> Observable<T>.delayedMessage(view: View, message: String): Observable<T> =
    compose(delayedMessageTransformer(view, message))

fun <T : Any> Single<T>.delayedMessage(view: View, message: String): Single<T> =
    compose(delayedMessageTransformer(view, message))

fun <T : Any> Maybe<T>.delayedMessage(view: View, message: String): Maybe<T> =
    compose(delayedMessageTransformer(view, message))

fun Completable.delayedMessage(view: View, message: String): Completable =
    compose(delayedMessageTransformer<Any>(view, message))

fun <T> delayedMessageTransformer(view: View, message: String): OmniTransformer<T, T> =
    object : OmniTransformer<T, T>() {

      private val timer = Observable.timer(300, TimeUnit.MILLISECONDS)
      private var snackbar: Snackbar? = null

      override fun apply(upstream: Observable<T>): Observable<T> {
        verifyMainThread()
        return upstream
            .doOnSubscribe {
              timer.takeUntil(upstream)
                  .subscribe {
                    snackbar = Snackbar.make(view, message, Snackbar.LENGTH_INDEFINITE).apply {
                      show()
                    }
                  }
            }
            .doOnTerminate {
              snackbar?.dismiss()
              snackbar = null
            }
      }

      override fun apply(upstream: Completable): CompletableSource {
        verifyMainThread()
        return upstream
            .doOnSubscribe {
              timer.takeUntil(upstream.toObservable<Any>())
                  .subscribe {
                    snackbar = Snackbar.make(view, message, Snackbar.LENGTH_INDEFINITE).apply {
                      show()
                    }
                  }
            }
            .doOnTerminate {
              snackbar?.dismiss()
              snackbar = null
            }
      }
    }

/**
 * Utility for working with enums when you want to run actions only on specific values
 */
inline fun <T : Enum<T>> Observable<T>.doOn(target: T,
    crossinline action: () -> Unit): Observable<T> = apply {
  doOnNext { if (it == target) action() }
}
