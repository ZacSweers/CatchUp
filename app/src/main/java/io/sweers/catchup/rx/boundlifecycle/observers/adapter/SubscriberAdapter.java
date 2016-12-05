package io.sweers.catchup.rx.boundlifecycle.observers.adapter;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public abstract class SubscriberAdapter<T> implements Subscriber<T> {
  public SubscriberAdapter() {
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
  public void onSubscribe(Subscription s) {

  }

  @Override
  public void onNext(T t) {

  }

  @Override
  public void onError(Throwable t) {

  }

  @Override
  public void onComplete() {

  }
}
