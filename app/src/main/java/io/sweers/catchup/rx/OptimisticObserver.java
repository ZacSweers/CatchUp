package io.sweers.catchup.rx;

import rx.Observer;
import timber.log.Timber;

public class OptimisticObserver<T> implements Observer<T> {

  private String tag;

  public OptimisticObserver(String tag) {
    this.tag = tag;
  }

  @Override
  public void onCompleted() {

  }

  @Override
  public void onError(Throwable e) {
    Timber.e(e, tag);
  }

  @Override
  public void onNext(T t) {

  }
}
