package io.sweers.catchup.ui.base;

import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import com.jakewharton.rxrelay.BehaviorRelay;
import com.trello.rxlifecycle.OutsideLifecycleException;
import com.trello.rxlifecycle.android.ActivityEvent;
import hu.akarnokd.rxjava.interop.RxJavaInterop;
import io.reactivex.Observable;
import io.reactivex.functions.Function;
import io.sweers.catchup.rx.boundlifecycle.LifecycleProvider;
import javax.annotation.Nonnull;

public class BaseActivity extends AppCompatActivity implements LifecycleProvider<ActivityEvent> {

  private static final Function<ActivityEvent, ActivityEvent> ACTIVITY_LIFECYCLE = lastEvent -> {
    switch (lastEvent) {
      case CREATE:
        return ActivityEvent.DESTROY;
      case START:
        return ActivityEvent.STOP;
      case RESUME:
        return ActivityEvent.PAUSE;
      case PAUSE:
        return ActivityEvent.STOP;
      case STOP:
        return ActivityEvent.DESTROY;
      case DESTROY:
        throw new OutsideLifecycleException("Cannot bind to Activity lifecycle when outside of it.");
      default:
        throw new UnsupportedOperationException("Binding to " + lastEvent + " not yet implemented");
    }
  };

  private final BehaviorRelay<ActivityEvent> lifecycleSubject = BehaviorRelay.create();

  @NonNull
  @CheckResult
  public final Observable<ActivityEvent> lifecycle() {
    return RxJavaInterop.toV2Observable(lifecycleSubject.asObservable());
  }

  @Nonnull
  @Override
  public Function<ActivityEvent, ActivityEvent> correspondingEvents() {
    return ACTIVITY_LIFECYCLE;
  }

  @Override
  public boolean hasLifecycleStarted() {
    return lifecycleSubject.getValue() != null;
  }

  @Override
  @CallSuper
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    lifecycleSubject.call(ActivityEvent.CREATE);
  }

  @Override
  @CallSuper
  protected void onStart() {
    super.onStart();
    lifecycleSubject.call(ActivityEvent.START);
  }

  @Override
  @CallSuper
  protected void onResume() {
    super.onResume();
    lifecycleSubject.call(ActivityEvent.RESUME);
  }

  @Override
  @CallSuper
  protected void onPause() {
    lifecycleSubject.call(ActivityEvent.PAUSE);
    super.onPause();
  }

  @Override
  @CallSuper
  protected void onStop() {
    lifecycleSubject.call(ActivityEvent.STOP);
    super.onStop();
  }

  @Override
  @CallSuper
  protected void onDestroy() {
    lifecycleSubject.call(ActivityEvent.DESTROY);
    super.onDestroy();
  }
}
