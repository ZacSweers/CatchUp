package io.sweers.catchup.rx.boundlifecycle.observers.adapter;

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
  public final String toString() {
    return super.toString();
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
