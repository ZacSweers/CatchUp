package io.sweers.catchup.ui.base;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.util.Pair;
import android.view.View;
import com.bluelinelabs.conductor.Controller;
import io.reactivex.functions.Function;
import io.reactivex.subjects.BehaviorSubject;
import io.sweers.catchup.rx.autodispose.LifecycleProvider;
import javax.annotation.Nonnull;
import rx.Observable;

public abstract class BaseController extends RefWatchingController
    implements LifecycleProvider<ControllerEvent> {

  private BehaviorSubject<ControllerEvent> lifecycleSubject =
      BehaviorSubject.createDefault(ControllerEvent.CREATE);

  protected BaseController() {
    super();
    initLifecycleHandling();
  }

  protected BaseController(Bundle args) {
    super(args);
    initLifecycleHandling();
  }

  private void initLifecycleHandling() {
    addLifecycleListener(new Controller.LifecycleListener() {
      @Override
      public void preCreateView(@NonNull Controller controller) {
        lifecycleSubject.onNext(ControllerEvent.CREATE_VIEW);
      }

      @Override
      public void preAttach(@NonNull Controller controller, @NonNull View view) {
        lifecycleSubject.onNext(ControllerEvent.ATTACH);
      }

      @Override
      public void preDetach(@NonNull Controller controller, @NonNull View view) {
        lifecycleSubject.onNext(ControllerEvent.DETACH);
      }

      @Override
      public void preDestroyView(@NonNull Controller controller, @NonNull View view) {
        lifecycleSubject.onNext(ControllerEvent.DESTROY_VIEW);
      }

      @Override
      public void preDestroy(@NonNull Controller controller) {
        lifecycleSubject.onNext(ControllerEvent.DESTROY);
      }
    });
  }

  protected UrlTransformer transformUrl(String url) {
    return new UrlTransformer(url);
  }

  @Nonnull
  @Override
  public io.reactivex.Observable<ControllerEvent> lifecycle() {
    return lifecycleSubject;
  }

  @Nonnull
  @Override
  public Function<ControllerEvent, ControllerEvent> correspondingEvents() {
    return ControllerEvent.LIFECYCLE;
  }

  @Override
  public ControllerEvent peekLifecycle() {
    return lifecycleSubject.getValue();
  }

  private class UrlTransformer implements Observable.Transformer<Object, Pair<String, Integer>> {

    private final String url;

    public UrlTransformer(String url) {
      this.url = url;
    }

    @Override
    public Observable<Pair<String, Integer>> call(Observable<Object> source) {
      return source.map(o -> Pair.create(url, getServiceThemeColor()));
    }
  }
}
