package io.sweers.catchup.rx;

import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Observable.Transformer;
import rx.functions.Action0;
import rx.schedulers.Schedulers;

public final class Transformers {
  private Transformers() {
    throw new InstantiationError();
  }

  public static <T> Transformer<T, T> doOnEmpty(Action0 action) {
    return source -> source
        .switchIfEmpty(Observable.<T>empty().doOnCompleted(action));
  }

  public static <T> Transformer<T, T> normalize(long time, TimeUnit unit) {
    return source -> source
        .lift(new OperatorNormalize<>(time, unit, Schedulers.computation()));
  }
}
