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
