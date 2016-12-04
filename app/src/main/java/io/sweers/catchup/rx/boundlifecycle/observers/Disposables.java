package io.sweers.catchup.rx.boundlifecycle.observers;

import android.support.annotation.NonNull;
import io.reactivex.CompletableObserver;
import io.reactivex.Maybe;
import io.reactivex.MaybeObserver;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.SingleObserver;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.sweers.catchup.rx.boundlifecycle.LifecycleProvider;
import org.reactivestreams.Subscriber;

/**
 * Created by pandanomic to 12/3/16.
 */
public final class Disposables {

  private final Maybe<?> lifecycle;

  public static Subscribers forFlowable(@NonNull LifecycleProvider<?> provider) {
    return new Subscribers(provider);
  }

  public static Subscribers forFlowable(@NonNull Observable<?> lifecycle) {
    return new Subscribers(lifecycle);
  }

  public static Subscribers forFlowable(@NonNull Maybe<?> lifecycle) {
    return new Subscribers(lifecycle);
  }

  public static Observers forObservable(@NonNull LifecycleProvider<?> provider) {
    return new Observers(provider);
  }

  public static Observers forObservable(@NonNull Observable<?> lifecycle) {
    return new Observers(lifecycle);
  }

  public static Observers forObservable(@NonNull Maybe<?> lifecycle) {
    return new Observers(lifecycle);
  }

  public static SingleObservers forSingle(@NonNull LifecycleProvider<?> provider) {
    return new SingleObservers(provider);
  }

  public static SingleObservers forSingle(@NonNull Observable<?> lifecycle) {
    return new SingleObservers(lifecycle);
  }

  public static SingleObservers forSingle(@NonNull Maybe<?> lifecycle) {
    return new SingleObservers(lifecycle);
  }

  public static MaybeObservers forMaybe(@NonNull LifecycleProvider<?> provider) {
    return new MaybeObservers(provider);
  }

  public static MaybeObservers forMaybe(@NonNull Observable<?> lifecycle) {
    return new MaybeObservers(lifecycle);
  }

  public static MaybeObservers forMaybe(@NonNull Maybe<?> lifecycle) {
    return new MaybeObservers(lifecycle);
  }

  public static CompletableObservers forCompletable(@NonNull LifecycleProvider<?> provider) {
    return new CompletableObservers(provider);
  }

  public static CompletableObservers forCompletable(@NonNull Observable<?> lifecycle) {
    return new CompletableObservers(lifecycle);
  }

  public static CompletableObservers forCompletable(@NonNull Maybe<?> lifecycle) {
    return new CompletableObservers(lifecycle);
  }

  private Disposables(@NonNull LifecycleProvider<?> provider) {
    this.lifecycle = BaseObserver.mapEvents(provider);
  }

  private Disposables(@NonNull Observable<?> lifecycle) {
    this.lifecycle = lifecycle.firstElement();
  }

  private Disposables(@NonNull Maybe<?> lifecycle) {
    this.lifecycle = lifecycle;
  }

  private static class Base {
    protected final Maybe<?> lifecycle;

    protected Base(@NonNull LifecycleProvider<?> provider) {
      this.lifecycle = BaseObserver.mapEvents(provider);
    }

    protected Base(@NonNull Observable<?> lifecycle) {
      this.lifecycle = lifecycle.firstElement();
    }

    protected Base(@NonNull Maybe<?> lifecycle) {
      this.lifecycle = lifecycle;
    }
  }

  public static class Subscribers extends Base {
    private Subscribers(@NonNull LifecycleProvider<?> provider) {
      super(provider);
    }

    private Subscribers(@NonNull Observable<?> lifecycle) {
      super(lifecycle);
    }

    private Subscribers(@NonNull Maybe<?> lifecycle) {
      super(lifecycle);
    }

    public <T> Subscriber<T> around(@NonNull Subscriber<T> o) {
      return new BoundSubscriber.Creator<T>(lifecycle).around(o);
    }

    public <T> Subscriber<T> around(@NonNull Consumer<T> o) {
      return new BoundSubscriber.Creator<T>(lifecycle).asConsumer(o);
    }

    public <T> Subscriber<T> around(@NonNull String errorTag, @NonNull Consumer<T> o) {
      return new BoundSubscriber.Creator<T>(lifecycle).asConsumer(errorTag, o);
    }
  }

  public static class Observers extends Base {
    private Observers(@NonNull LifecycleProvider<?> provider) {
      super(provider);
    }

    private Observers(@NonNull Observable<?> lifecycle) {
      super(lifecycle);
    }

    private Observers(@NonNull Maybe<?> lifecycle) {
      super(lifecycle);
    }

    public <T> Observer<T> around(@NonNull Observer<T> o) {
      return new BoundObserver.Creator<T>(lifecycle).around(o);
    }

    public <T> Observer<T> around(@NonNull Consumer<T> o) {
      return new BoundObserver.Creator<T>(lifecycle).asConsumer(o);
    }

    public <T> Observer<T> around(@NonNull String errorTag, @NonNull Consumer<T> o) {
      return new BoundObserver.Creator<T>(lifecycle).asConsumer(errorTag, o);
    }
  }

  public static class SingleObservers extends Base {
    private SingleObservers(@NonNull LifecycleProvider<?> provider) {
      super(provider);
    }

    private SingleObservers(@NonNull Observable<?> lifecycle) {
      super(lifecycle);
    }

    private SingleObservers(@NonNull Maybe<?> lifecycle) {
      super(lifecycle);
    }

    public <T> SingleObserver<T> around(@NonNull SingleObserver<T> o) {
      return new BoundSingleObserver.Creator<T>(lifecycle).around(o);
    }

    public <T> SingleObserver<T> around(@NonNull Consumer<T> o) {
      return new BoundSingleObserver.Creator<T>(lifecycle).asConsumer(o);
    }

    public <T> SingleObserver<T> around(@NonNull String errorTag, @NonNull Consumer<T> o) {
      return new BoundSingleObserver.Creator<T>(lifecycle).asConsumer(errorTag, o);
    }
  }

  public static class MaybeObservers extends Base {
    private MaybeObservers(@NonNull LifecycleProvider<?> provider) {
      super(provider);
    }

    private MaybeObservers(@NonNull Observable<?> lifecycle) {
      super(lifecycle);
    }

    private MaybeObservers(@NonNull Maybe<?> lifecycle) {
      super(lifecycle);
    }

    public <T> MaybeObserver<T> around(@NonNull MaybeObserver<T> o) {
      return new BoundMaybeObserver.Creator<T>(lifecycle).around(o);
    }

    public <T> MaybeObserver<T> around(@NonNull Consumer<T> o) {
      return new BoundMaybeObserver.Creator<T>(lifecycle).asConsumer(o);
    }

    public <T> MaybeObserver<T> around(@NonNull String errorTag, @NonNull Consumer<T> o) {
      return new BoundMaybeObserver.Creator<T>(lifecycle).asConsumer(errorTag, o);
    }
  }

  public static class CompletableObservers extends Base {
    private CompletableObservers(@NonNull LifecycleProvider<?> provider) {
      super(provider);
    }

    private CompletableObservers(@NonNull Observable<?> lifecycle) {
      super(lifecycle);
    }

    private CompletableObservers(@NonNull Maybe<?> lifecycle) {
      super(lifecycle);
    }

    public CompletableObserver around(@NonNull CompletableObserver o) {
      return new BoundCompletableObserver.Creator(lifecycle).around(o);
    }

    public CompletableObserver around(@NonNull Action a) {
      return new BoundCompletableObserver.Creator(lifecycle).asAction(a);
    }

    public CompletableObserver around(@NonNull String errorTag, @NonNull Action a) {
      return new BoundCompletableObserver.Creator(lifecycle).asAction(errorTag, a);
    }
  }
}
