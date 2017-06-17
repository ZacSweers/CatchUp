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

package io.sweers.catchup.rx.observers.adapter

import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription

abstract class SubscriberAdapter<T> : Subscriber<T> {

  override fun hashCode(): Int {
    return super.hashCode()
  }

  override fun equals(other: Any?): Boolean {
    return super.equals(other)
  }

  override fun onSubscribe(s: Subscription) {

  }

  override fun onNext(t: T) {

  }

  override fun onError(t: Throwable) {

  }

  override fun onComplete() {

  }
}
