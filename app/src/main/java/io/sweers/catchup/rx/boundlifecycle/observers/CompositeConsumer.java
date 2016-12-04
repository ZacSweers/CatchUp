package io.sweers.catchup.rx.boundlifecycle.observers;

import io.reactivex.CompletableObserver;
import io.reactivex.MaybeObserver;
import io.reactivex.SingleObserver;
import io.reactivex.functions.Consumer;

// yolo type safety
public interface CompositeConsumer<T> extends Consumer<T> {

  MaybeObserver<T> forMaybe();
  SingleObserver<T> forSingle();
  CompletableObserver forCompletable();

}
