package io.sweers.catchup.rx;

import android.support.design.widget.Snackbar;
import android.view.View;

import java.util.concurrent.TimeUnit;

import io.reactivex.Flowable;
import io.reactivex.FlowableTransformer;
import io.reactivex.functions.Action;
import rx.Observable.Transformer;
import rx.schedulers.Schedulers;

import static rx.android.MainThreadSubscription.verifyMainThread;

public final class Transformers {
  private Transformers() {
    throw new InstantiationError();
  }

  public static <T> FlowableTransformer<T, T> doOnEmpty(Action action) {
    return source -> source
        .switchIfEmpty(Flowable.<T>empty().doOnComplete(action));
  }

  public static <T> Transformer<T, T> normalize(long time, TimeUnit unit) {
    return source -> source
        .lift(new OperatorNormalize<>(time, unit, Schedulers.computation()));
  }

  public static <T> FlowableTransformer<T, T> delayedMessage(View view, String message) {
    return new FlowableTransformer<T, T>() {

      private Flowable<Long> timer = Flowable.timer(300, TimeUnit.MILLISECONDS);
      private Snackbar snackbar = null;

      @Override
      public Flowable<T> apply(Flowable<T> source) {
        verifyMainThread();
        return source
            .doOnSubscribe(c -> timer.takeUntil(source)
                .subscribe(aLong -> {
                  snackbar = Snackbar.make(view, message, Snackbar.LENGTH_INDEFINITE);
                  snackbar.show();
                }))
            .doOnTerminate(() -> {
              if (snackbar != null) {
                snackbar.dismiss();
                snackbar = null;
              }
            });
      }
    };
  }
}
