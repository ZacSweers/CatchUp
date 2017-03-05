package io.sweers.catchup.ui.base;

import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import com.bluelinelabs.conductor.Controller;
import com.jakewharton.rxrelay2.BehaviorRelay;
import com.uber.autodispose.LifecycleScopeProvider;
import dagger.android.AndroidInjection;
import dagger.android.DispatchingAndroidInjector;
import io.reactivex.Observable;
import io.reactivex.functions.Function;
import io.sweers.catchup.injection.HasDispatchingControllerInjector;
import javax.inject.Inject;

public class BaseActivity extends AppCompatActivity
    implements LifecycleScopeProvider<ActivityEvent>, HasDispatchingControllerInjector {

  @Inject DispatchingAndroidInjector<Controller> controllerInjector;
  private final BehaviorRelay<ActivityEvent> lifecycleRelay = BehaviorRelay.create();

  @NonNull @CheckResult @Override public final Observable<ActivityEvent> lifecycle() {
    return lifecycleRelay;
  }

  @NonNull @Override public Function<ActivityEvent, ActivityEvent> correspondingEvents() {
    return ActivityEvent.LIFECYCLE;
  }

  @Override public ActivityEvent peekLifecycle() {
    return lifecycleRelay.getValue();
  }

  @Override @CallSuper protected void onCreate(Bundle savedInstanceState) {
    AndroidInjection.inject(this);
    super.onCreate(savedInstanceState);
    lifecycleRelay.accept(ActivityEvent.CREATE);
  }

  @Override @CallSuper protected void onStart() {
    super.onStart();
    lifecycleRelay.accept(ActivityEvent.START);
  }

  @Override @CallSuper protected void onResume() {
    super.onResume();
    lifecycleRelay.accept(ActivityEvent.RESUME);
  }

  @Override @CallSuper protected void onPause() {
    lifecycleRelay.accept(ActivityEvent.PAUSE);
    super.onPause();
  }

  @Override @CallSuper protected void onStop() {
    lifecycleRelay.accept(ActivityEvent.STOP);
    super.onStop();
  }

  @Override @CallSuper protected void onDestroy() {
    lifecycleRelay.accept(ActivityEvent.DESTROY);
    super.onDestroy();
  }

  @Override public DispatchingAndroidInjector<Controller> controllerInjector() {
    return controllerInjector;
  }
}
