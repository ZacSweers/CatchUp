package io.sweers.catchup.ui.base;

import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import com.jakewharton.rxrelay2.BehaviorRelay;
import io.reactivex.Observable;
import io.reactivex.functions.Function;
import io.sweers.catchup.rx.boundlifecycle.LifecycleProvider;
import javax.annotation.Nonnull;

public class BaseActivity extends AppCompatActivity implements LifecycleProvider<ActivityEvent> {

  private final BehaviorRelay<ActivityEvent> lifecycleRelay = BehaviorRelay.create();

  @NonNull
  @CheckResult
  public final Observable<ActivityEvent> lifecycle() {
    return lifecycleRelay;
  }

  @Nonnull
  @Override
  public Function<ActivityEvent, ActivityEvent> correspondingEvents() {
    return ActivityEvent.LIFECYCLE;
  }

  @Override
  public ActivityEvent peekLifecycle() {
    return lifecycleRelay.getValue();
  }

  @Override
  @CallSuper
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    lifecycleRelay.accept(ActivityEvent.CREATE);
  }

  @Override
  @CallSuper
  protected void onStart() {
    super.onStart();
    lifecycleRelay.accept(ActivityEvent.START);
  }

  @Override
  @CallSuper
  protected void onResume() {
    super.onResume();
    lifecycleRelay.accept(ActivityEvent.RESUME);
  }

  @Override
  @CallSuper
  protected void onPause() {
    lifecycleRelay.accept(ActivityEvent.PAUSE);
    super.onPause();
  }

  @Override
  @CallSuper
  protected void onStop() {
    lifecycleRelay.accept(ActivityEvent.STOP);
    super.onStop();
  }

  @Override
  @CallSuper
  protected void onDestroy() {
    lifecycleRelay.accept(ActivityEvent.DESTROY);
    super.onDestroy();
  }
}
