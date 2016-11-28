package io.sweers.catchup.ui.base;

import android.os.Bundle;
import android.support.v4.util.Pair;
import com.bluelinelabs.conductor.rxlifecycle.ControllerEvent;
import com.trello.rxlifecycle.OutsideLifecycleException;
import io.reactivex.functions.Function;
import io.reactivex.subjects.BehaviorSubject;
import io.sweers.catchup.rx.boundlifecycle.LifecycleProvider;
import javax.annotation.Nonnull;
import rx.Observable;

public abstract class BaseController extends RefWatchingController
    implements LifecycleProvider<ControllerEvent> {

  private BehaviorSubject<ControllerEvent> lifecycleSubject = BehaviorSubject.create();
  private static final Function<ControllerEvent, ControllerEvent> CONTROLLER_LIFECYCLE =
      lastEvent -> {
        switch (lastEvent) {
          case CREATE:
            return ControllerEvent.DESTROY;
          case ATTACH:
            return ControllerEvent.DETACH;
          case CREATE_VIEW:
            return ControllerEvent.DESTROY_VIEW;
          case DETACH:
            return ControllerEvent.DESTROY;
          default:
            throw new OutsideLifecycleException(
                "Cannot bind to Controller lifecycle when outside of it.");
        }
      };

  protected BaseController() {
  }

  protected BaseController(Bundle args) {
    super(args);
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
    return CONTROLLER_LIFECYCLE;
  }

  @Override
  public boolean hasLifecycleStarted() {
    return lifecycleSubject.getValue() != null;
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
