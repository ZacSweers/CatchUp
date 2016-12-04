package io.sweers.catchup.rx.boundlifecycle.observers;

import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.SingleObserver;
import io.reactivex.functions.Consumer;
import io.sweers.catchup.rx.boundlifecycle.LifecycleProvider;

/**
 * Created by pandanomic to 12/3/16.
 */
public class BoundObservers2 {

  private Maybe<?> lifecycle;

  public static BoundObservers2 against(LifecycleProvider<?> provider) {
    return new BoundObservers2(provider);
  }

  public static BoundObservers2 against(Observable<?> lifecycle) {
    return new BoundObservers2(lifecycle);
  }

  public static BoundObservers2 against(Maybe<?> lifecycle) {
    return new BoundObservers2(lifecycle);
  }

  BoundObservers2(LifecycleProvider<?> provider) {
    this.lifecycle = BaseObserver.mapEvents(provider);
  }

  BoundObservers2(Observable<?> lifecycle) {
    this.lifecycle = lifecycle.firstElement();
  }

  BoundObservers2(Maybe<?> lifecycle) {
    this.lifecycle = lifecycle;
  }

  public <T> Observer<T> around(Observer<T> o) {
    return new BoundObserver.Creator<T>(lifecycle).around(o);
  }

  public <T> Observer<T> aroundConsumer(Consumer<T> o) {
    return new BoundObserver.Creator<T>(lifecycle).asConsumer(o);
  }

  public <T> SingleObserver<T> around(SingleObserver<T> o) {
    return new BoundSingleObserver.Creator<T>(lifecycle).around(o);
  }

  public <T> Observer<T> aroundConsumerSingle(Consumer<T> o) {
    return new BoundObserver.Creator<T>(lifecycle).asConsumer(o);
  }
}
