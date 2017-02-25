package io.sweers.catchup.rx;

import android.support.design.widget.Snackbar;
import android.view.View;
import io.reactivex.Completable;
import io.reactivex.CompletableSource;
import io.reactivex.CompletableTransformer;
import io.reactivex.Maybe;
import io.reactivex.MaybeSource;
import io.reactivex.MaybeTransformer;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.reactivex.SingleTransformer;
import io.reactivex.functions.Action;
import java.util.concurrent.TimeUnit;
import rx.Observable.Transformer;
import rx.schedulers.Schedulers;

import static rx.android.MainThreadSubscription.verifyMainThread;

public final class Transformers {
  private Transformers() {
    throw new InstantiationError();
  }

  public static <T> ObservableTransformer<T, T> doOnEmpty(Action action) {
    return source -> source.switchIfEmpty(Observable.<T>empty().doOnComplete(action));
  }

  public static <T> Transformer<T, T> normalize(long time, TimeUnit unit) {
    return source -> source.lift(new OperatorNormalize<>(time, unit, Schedulers.computation()));
  }

  public static <T> OmniTransformer<T, T> delayedMessage(View view, String message) {
    return new OmniTransformer<T, T>() {

      private Observable<Long> timer = Observable.timer(300, TimeUnit.MILLISECONDS);
      private Snackbar snackbar = null;

      @Override public Observable<T> apply(Observable<T> source) {
        verifyMainThread();
        return source.doOnSubscribe(c -> timer.takeUntil(source)
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

      @Override public CompletableSource apply(Completable upstream) {
        verifyMainThread();
        return upstream.doOnSubscribe(c -> timer.takeUntil(upstream.toObservable())
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

  public static abstract class OmniTransformer<Upstream, Downstream>
      implements ObservableTransformer<Upstream, Downstream>,
      SingleTransformer<Upstream, Downstream>, MaybeTransformer<Upstream, Downstream>,
      CompletableTransformer {

    @Override public CompletableSource apply(Completable upstream) {
      return upstream;
    }

    @Override public MaybeSource<Downstream> apply(Maybe<Upstream> upstream) {
      return (MaybeSource<Downstream>) upstream;
    }

    @Override public ObservableSource<Downstream> apply(Observable<Upstream> upstream) {
      return (ObservableSource<Downstream>) upstream;
    }

    @Override public SingleSource<Downstream> apply(Single<Upstream> upstream) {
      return (SingleSource<Downstream>) upstream;
    }
  }
}
