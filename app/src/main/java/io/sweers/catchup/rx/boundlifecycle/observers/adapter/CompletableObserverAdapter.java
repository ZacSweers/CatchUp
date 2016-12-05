package io.sweers.catchup.rx.boundlifecycle.observers.adapter;

import io.reactivex.CompletableObserver;
import io.reactivex.disposables.Disposable;

public abstract class CompletableObserverAdapter implements CompletableObserver {
  public CompletableObserverAdapter() {
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
  public void onComplete() {

  }

  @Override
  public void onError(Throwable e) {

  }
}
