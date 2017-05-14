/*
 * Copyright (c) 2017 Zac Sweers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

public abstract class BaseActivity extends AppCompatActivity
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
