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

package io.sweers.catchup.rx;

import io.reactivex.functions.Consumer;
import io.reactivex.functions.Predicate;

/**
 * A consumer that only calls the {@link #accept(Object)} method if {@link #test(Object)} is true.
 */
public abstract class PredicateConsumer<T> implements Consumer<T>, Predicate<T> {
  @Override public final void accept(T t) throws Exception {
    if (test(t)) {
      acceptActual(t);
    }
  }

  public abstract void acceptActual(T t) throws Exception;
}
