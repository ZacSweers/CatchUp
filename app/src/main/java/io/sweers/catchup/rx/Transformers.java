package io.sweers.catchup.rx;

import android.support.design.widget.Snackbar;
import android.view.View;

import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Observable.Transformer;
import rx.functions.Action0;
import rx.schedulers.Schedulers;

import static rx.android.MainThreadSubscription.verifyMainThread;

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

  public static <T> Transformer<T, T> delayedMessage(View view, String message) {
    return new Transformer<T, T>() {

      private Observable<Long> timer = Observable.timer(300, TimeUnit.MILLISECONDS);
      private Snackbar snackbar = null;

      @Override
      public Observable<T> call(Observable<T> source) {
        verifyMainThread();
        return source
            .doOnSubscribe(() -> timer.takeUntil(source)
                .subscribe(aLong -> {
                  snackbar = Snackbar.make(view, message, Snackbar.LENGTH_INDEFINITE);
                  snackbar.show();
                }))
            .doOnUnsubscribe(() -> {
              if (snackbar != null) {
                snackbar.dismiss();
                snackbar = null;
              }
            });
      }
    };
  }
}
