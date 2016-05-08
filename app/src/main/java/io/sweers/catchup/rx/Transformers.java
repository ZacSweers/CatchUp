package io.sweers.catchup.rx;

import rx.Observable;
import rx.Observable.Transformer;
import rx.functions.Action0;

public final class Transformers {
  private Transformers() {
    throw new InstantiationError();
  }

  public static <T> Transformer<T, T> doOnEmpty(Action0 action) {
    return source -> source
        .switchIfEmpty(Observable.<T>empty().doOnCompleted(action));
  }
}
