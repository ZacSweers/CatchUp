package io.sweers.catchup.rx.observers.adapter;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

public abstract class ObserverAdapter<T> implements Observer<T> {
  public ObserverAdapter() {
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
  public void onNext(T value) {

  }

  @Override
  public void onError(Throwable e) {

  }

  @Override
  public void onComplete() {

  }
}
