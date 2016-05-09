package io.sweers.catchup.rx;

import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.view.View;

import com.bluelinelabs.conductor.rxlifecycle.ControllerEvent;
import com.bluelinelabs.conductor.rxlifecycle.RxController;
import com.trello.rxlifecycle.LifecycleTransformer;
import com.trello.rxlifecycle.OutsideLifecycleException;
import com.trello.rxlifecycle.RxLifecycle;

import io.sweers.catchup.ui.base.BaseActivity;
import rx.functions.Func1;

public final class Confine {
  // TODO Remove this when Conductor's updated again to support latest RxLifecycle
  private static final Func1<ControllerEvent, ControllerEvent> CONTROLLER_LIFECYCLE =
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
            throw new OutsideLifecycleException("Cannot bind to Controller lifecycle when outside of it.");
        }
      };

  private Confine() {
    throw new InstantiationError();
  }

  @NonNull
  @CheckResult
  public static <T> LifecycleTransformer<T> to(@NonNull BaseActivity activity) {
    return RxLifecycle.bindActivity(activity.lifecycle());
  }

  @NonNull
  @CheckResult
  public static <T> LifecycleTransformer<T> to(@NonNull RxController controller) {
    return RxLifecycle.bind(controller.lifecycle(), CONTROLLER_LIFECYCLE);
  }

  @NonNull
  @CheckResult
  public static <T> LifecycleTransformer<T> to(@NonNull View view) {
    return RxLifecycle.bindView(view);
  }
}
