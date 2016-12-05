package io.sweers.catchup.rx.boundlifecycle.observers;

import io.reactivex.CompletableObserver;
import io.reactivex.Maybe;
import io.reactivex.MaybeObserver;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.SingleObserver;
import io.reactivex.functions.Action;
import io.reactivex.functions.BiConsumer;
import io.reactivex.functions.Consumer;
import io.sweers.catchup.rx.boundlifecycle.LifecycleProvider;
import org.reactivestreams.Subscriber;

public final class Disposables {

  private final Maybe<?> lifecycle;

  public static Subscribers forFlowable(LifecycleProvider<?> provider) {
    return new Subscribers(provider);
  }

  public static Subscribers forFlowable(Observable<?> lifecycle) {
    return new Subscribers(lifecycle);
  }

  public static Subscribers forFlowable(Maybe<?> lifecycle) {
    return new Subscribers(lifecycle);
  }

  public static Observers forObservable(LifecycleProvider<?> provider) {
    return new Observers(provider);
  }

  public static Observers forObservable(Observable<?> lifecycle) {
    return new Observers(lifecycle);
  }

  public static Observers forObservable(Maybe<?> lifecycle) {
    return new Observers(lifecycle);
  }

  public static SingleObservers forSingle(LifecycleProvider<?> provider) {
    return new SingleObservers(provider);
  }

  public static SingleObservers forSingle(Observable<?> lifecycle) {
    return new SingleObservers(lifecycle);
  }

  public static SingleObservers forSingle(Maybe<?> lifecycle) {
    return new SingleObservers(lifecycle);
  }

  public static MaybeObservers forMaybe(LifecycleProvider<?> provider) {
    return new MaybeObservers(provider);
  }

  public static MaybeObservers forMaybe(Observable<?> lifecycle) {
    return new MaybeObservers(lifecycle);
  }

  public static MaybeObservers forMaybe(Maybe<?> lifecycle) {
    return new MaybeObservers(lifecycle);
  }

  public static CompletableObservers forCompletable(LifecycleProvider<?> provider) {
    return new CompletableObservers(provider);
  }

  public static CompletableObservers forCompletable(Observable<?> lifecycle) {
    return new CompletableObservers(lifecycle);
  }

  public static CompletableObservers forCompletable(Maybe<?> lifecycle) {
    return new CompletableObservers(lifecycle);
  }

  private Disposables(LifecycleProvider<?> provider) {
    this.lifecycle = Util.mapEvents(provider);
  }

  private Disposables(Observable<?> lifecycle) {
    this.lifecycle = lifecycle.firstElement();
  }

  private Disposables(Maybe<?> lifecycle) {
    this.lifecycle = lifecycle;
  }

  private static class Base {
    protected final Maybe<?> lifecycle;

    protected Base(LifecycleProvider<?> provider) {
      this.lifecycle = Util.mapEvents(provider);
    }

    protected Base(Observable<?> lifecycle) {
      this.lifecycle = lifecycle.firstElement();
    }

    protected Base(Maybe<?> lifecycle) {
      this.lifecycle = lifecycle;
    }
  }

  public static class Subscribers extends Base {
    private Subscribers(LifecycleProvider<?> provider) {
      super(provider);
    }

    private Subscribers(Observable<?> lifecycle) {
      super(lifecycle);
    }

    private Subscribers(Maybe<?> lifecycle) {
      super(lifecycle);
    }

    public <T> Subscriber<T> around(Subscriber<T> o) {
      return new BoundSubscriber.Creator<T>(lifecycle).around(o);
    }

    public <T> Subscriber<T> around(Consumer<T> o) {
      return new BoundSubscriber.Creator<T>(lifecycle).asConsumer(o);
    }

    public <T> Subscriber<T> around(String errorTag, Consumer<T> o) {
      return new BoundSubscriber.Creator<T>(lifecycle).asConsumer(errorTag, o);
    }
  }

  public static class Observers extends Base {
    private Observers(LifecycleProvider<?> provider) {
      super(provider);
    }

    private Observers(Observable<?> lifecycle) {
      super(lifecycle);
    }

    private Observers(Maybe<?> lifecycle) {
      super(lifecycle);
    }

    public <T> Observer<T> around(Observer<T> o) {
      return new BoundObserver.Creator<T>(lifecycle).around(o);
    }

    public <T> Observer<T> around(Consumer<T> o) {
      return new BoundObserver.Creator<T>(lifecycle).asConsumer(o);
    }

    public <T> Observer<T> around(String errorTag, Consumer<T> o) {
      return new BoundObserver.Creator<T>(lifecycle).asConsumer(errorTag, o);
    }
  }

  public static class SingleObservers extends Base {
    private SingleObservers(LifecycleProvider<?> provider) {
      super(provider);
    }

    private SingleObservers(Observable<?> lifecycle) {
      super(lifecycle);
    }

    private SingleObservers(Maybe<?> lifecycle) {
      super(lifecycle);
    }

    public <T> SingleObserver<? super T> around(SingleObserver<T> o) {
      return new BoundSingleObserver.Creator<T>(lifecycle).around(o);
    }

    public <T> SingleObserver<? super T> around(Consumer<? super T> o) {
      return new BoundSingleObserver.Creator<T>(lifecycle).asConsumer(o);
    }

    public <T> SingleObserver<? super T> around(String errorTag, Consumer<T> o) {
      return new BoundSingleObserver.Creator<T>(lifecycle).asConsumer(errorTag, o);
    }

    public <T> SingleObserver<? super T> around(Consumer<? super T> c, Consumer<Throwable> error) {
      return new BoundSingleObserver.Creator<T>(lifecycle).onError(error)
          .onSuccess(c)
          .create();
    }

    public <T> SingleObserver<? super T> around(BiConsumer<? super T, ? super Throwable> c) {
      return new BoundSingleObserver.Creator<T>(lifecycle).around(c);
    }
  }

  public static class MaybeObservers extends Base {
    private MaybeObservers(LifecycleProvider<?> provider) {
      super(provider);
    }

    private MaybeObservers(Observable<?> lifecycle) {
      super(lifecycle);
    }

    private MaybeObservers(Maybe<?> lifecycle) {
      super(lifecycle);
    }

    public <T> MaybeObserver<T> around(MaybeObserver<T> o) {
      return new BoundMaybeObserver.Creator<T>(lifecycle).around(o);
    }

    public <T> MaybeObserver<T> around(Consumer<T> o) {
      return new BoundMaybeObserver.Creator<T>(lifecycle).asConsumer(o);
    }

    public <T> MaybeObserver<T> around(String errorTag, Consumer<T> o) {
      return new BoundMaybeObserver.Creator<T>(lifecycle).asConsumer(errorTag, o);
    }
  }

  public static class CompletableObservers extends Base {
    private CompletableObservers(LifecycleProvider<?> provider) {
      super(provider);
    }

    private CompletableObservers(Observable<?> lifecycle) {
      super(lifecycle);
    }

    private CompletableObservers(Maybe<?> lifecycle) {
      super(lifecycle);
    }

    public CompletableObserver around(CompletableObserver o) {
      return new BoundCompletableObserver.Creator(lifecycle).around(o);
    }

    public CompletableObserver around(Action a) {
      return new BoundCompletableObserver.Creator(lifecycle).asAction(a);
    }

    public CompletableObserver around(String errorTag, Action a) {
      return new BoundCompletableObserver.Creator(lifecycle).asAction(errorTag, a);
    }
  }
}
