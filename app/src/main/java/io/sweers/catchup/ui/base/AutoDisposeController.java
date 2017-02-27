package io.sweers.catchup.ui.base;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import com.bluelinelabs.conductor.Controller;
import com.uber.autodispose.LifecycleScopeProvider;
import io.reactivex.functions.Function;
import io.reactivex.subjects.BehaviorSubject;

public abstract class AutoDisposeController extends RefWatchingController
    implements LifecycleScopeProvider<ControllerEvent> {

  private BehaviorSubject<ControllerEvent> lifecycleSubject =
      BehaviorSubject.createDefault(ControllerEvent.CREATE);

  protected AutoDisposeController() {
    super();
    initLifecycleHandling();
  }

  protected AutoDisposeController(Bundle args) {
    super(args);
    initLifecycleHandling();
  }

  private void initLifecycleHandling() {
    addLifecycleListener(new Controller.LifecycleListener() {
      @Override public void preCreateView(@NonNull Controller controller) {
        lifecycleSubject.onNext(ControllerEvent.CREATE_VIEW);
      }

      @Override public void preAttach(@NonNull Controller controller, @NonNull View view) {
        lifecycleSubject.onNext(ControllerEvent.ATTACH);
      }

      @Override public void preDetach(@NonNull Controller controller, @NonNull View view) {
        lifecycleSubject.onNext(ControllerEvent.DETACH);
      }

      @Override public void preDestroyView(@NonNull Controller controller, @NonNull View view) {
        lifecycleSubject.onNext(ControllerEvent.DESTROY_VIEW);
      }

      @Override public void preDestroy(@NonNull Controller controller) {
        lifecycleSubject.onNext(ControllerEvent.DESTROY);
      }
    });
  }

  @NonNull @Override public io.reactivex.Observable<ControllerEvent> lifecycle() {
    return lifecycleSubject;
  }

  @NonNull @Override public Function<ControllerEvent, ControllerEvent> correspondingEvents() {
    return ControllerEvent.LIFECYCLE;
  }

  @Override public ControllerEvent peekLifecycle() {
    return lifecycleSubject.getValue();
  }
}
