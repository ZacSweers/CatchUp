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

package io.sweers.catchup.rx.observers.adapter;

import io.reactivex.MaybeObserver;
import io.reactivex.disposables.Disposable;

public abstract class MaybeObserverAdapter<T> implements MaybeObserver<T> {
  public MaybeObserverAdapter() {
    super();
  }

  @Override
  public final int hashCode() {
    return super.hashCode();
  }

  @Override
  public final boolean equals(Object obj) {
    return super.equals(obj);
  }

  @Override
  protected final Object clone() throws CloneNotSupportedException {
    return super.clone();
  }

  @Override
  protected final void finalize() throws Throwable {
    super.finalize();
  }

  @Override
  public void onSubscribe(Disposable d) {

  }

  @Override
  public void onSuccess(T value) {

  }

  @Override
  public void onError(Throwable e) {

  }

  @Override
  public void onComplete() {

  }
}
