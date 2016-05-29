package io.sweers.catchup.rx;

import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.view.View;

import com.bluelinelabs.conductor.rxlifecycle.RxController;
import com.bluelinelabs.conductor.rxlifecycle.RxControllerLifecycle;
import com.trello.rxlifecycle.LifecycleTransformer;
import com.trello.rxlifecycle.RxLifecycle;

import io.sweers.catchup.ui.base.BaseActivity;

public final class Confine {

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
    return RxControllerLifecycle.bindController(controller.lifecycle());
  }

  @NonNull
  @CheckResult
  public static <T> LifecycleTransformer<T> to(@NonNull View view) {
    return RxLifecycle.bindView(view);
  }
}
