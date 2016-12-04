package io.sweers.catchup.rx.boundlifecycle.observers;

import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Observer;
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

  <E> BoundObservers2(LifecycleProvider<E> provider) {
    this.lifecycle = BaseObserver.mapEvents(provider.lifecycle(), provider.correspondingEvents());
  }

  <E> BoundObservers2(Observable<E> lifecycle) {
    this.lifecycle = lifecycle.firstElement();
  }

  <E> BoundObservers2(Maybe<E> lifecycle) {
    this.lifecycle = lifecycle;
  }

  public <T> Observer<T> around(Observer<T> o) {
    return new BoundObserver.BoundObserverCreator<T>(lifecycle).around(o);
  }
}
