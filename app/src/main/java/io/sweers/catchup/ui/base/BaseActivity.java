package io.sweers.catchup.ui.base;

import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;

import com.jakewharton.rxrelay.BehaviorRelay;
import com.trello.rxlifecycle.ActivityEvent;

import rx.Observable;

public class BaseActivity extends AppCompatActivity {

  private final BehaviorRelay<ActivityEvent> lifecycleSubject = BehaviorRelay.create();

  @NonNull
  @CheckResult
  public final Observable<ActivityEvent> lifecycle() {
    return lifecycleSubject.asObservable();
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
