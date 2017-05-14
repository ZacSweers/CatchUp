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

package io.sweers.catchup.ui;

import android.app.Activity;
import android.content.Context;
import android.os.PowerManager;
import android.support.v4.view.GravityCompat;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.f2prateek.rx.preferences.Preference;
import com.jakewharton.madge.MadgeFrameLayout;
import com.jakewharton.scalpel.ScalpelFrameLayout;
import com.mattprecious.telescope.TelescopeLayout;
import com.uber.autodispose.ObservableScoper;
import io.reactivex.functions.Consumer;
import io.sweers.catchup.P;
import io.sweers.catchup.R;
import io.sweers.catchup.data.LumberYard;
import io.sweers.catchup.ui.base.ActivityEvent;
import io.sweers.catchup.ui.base.BaseActivity;
import io.sweers.catchup.ui.debug.DebugDrawerLayout;
import io.sweers.catchup.ui.debug.DebugView;
import javax.inject.Inject;
import rx.subscriptions.CompositeSubscription;

import static android.content.Context.POWER_SERVICE;
import static android.os.PowerManager.ACQUIRE_CAUSES_WAKEUP;
import static android.os.PowerManager.FULL_WAKE_LOCK;
import static android.os.PowerManager.ON_AFTER_RELEASE;
import static android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED;

/**
 * An {@link ViewContainer} for debug builds which wraps a sliding drawer on the right that holds
 * all of the debug information and settings.
 */
public final class DebugViewContainer implements ViewContainer {
  private final LumberYard lumberYard;
  private Preference<Boolean> seenDebugDrawer = P.debugSeenDebugDrawer.rx();
  private Preference<Boolean> pixelGridEnabled = P.debugPixelGridEnabled.rx();
  private Preference<Boolean> pixelRatioEnabled = P.debugPixelRatioEnabled.rx();
  private Preference<Boolean> scalpelEnabled = P.debugScalpelEnabled.rx();
  private Preference<Boolean> scalpelWireframeEnabled = P.debugScalpelWireframeDrawer.rx();

  @Inject public DebugViewContainer(LumberYard lumberYard) {
    this.lumberYard = lumberYard;
  }

  /**
   * Show the activity over the lock-screen and wake up the device. If you launched the app manually
   * both of these conditions are already true. If you deployed from the IDE, however, this will
   * save you from hundreds of power button presses and pattern swiping per day!
   */
  public static void riseAndShine(Activity activity) {
    activity.getWindow().addFlags(FLAG_SHOW_WHEN_LOCKED);

    PowerManager power = (PowerManager) activity.getSystemService(POWER_SERVICE);
    PowerManager.WakeLock lock =
        power.newWakeLock(FULL_WAKE_LOCK | ACQUIRE_CAUSES_WAKEUP | ON_AFTER_RELEASE, "wakeup!");
    lock.acquire();
    lock.release();
  }

  @Override public ViewGroup forActivity(final BaseActivity activity) {
    activity.setContentView(R.layout.debug_activity_frame);

    final ViewHolder viewHolder = new ViewHolder();
    Unbinder unbinder = ButterKnife.bind(viewHolder, activity);

    final Context drawerContext = new ContextThemeWrapper(activity, R.style.DebugDrawer);
    final DebugView debugView = new DebugView(drawerContext);
    viewHolder.debugDrawer.addView(debugView);

    // Set up the contextual actions to watch views coming in and out of the content area.
    //    ContextualDebugActions contextualActions = debugView.getContextualDebugActions();
    //    contextualActions.setActionClickListener(v -> viewHolder.drawerLayout.closeDrawers());
    //    viewHolder.content.setOnHierarchyChangeListener(
    //        HierarchyTreeChangeListener.wrap(contextualActions));

    //    viewHolder.drawerLayout.setDrawerShadow(R.drawable.debug_drawer_shadow, GravityCompat.END);
    viewHolder.drawerLayout.setDrawerListener(new DebugDrawerLayout.SimpleDrawerListener() {
      @Override public void onDrawerOpened(View drawerView) {
        debugView.onDrawerOpened();
      }
    });

    viewHolder.telescopeLayout.setPointerCount(3);
    TelescopeLayout.cleanUp(activity); // Clean up any old screenshots.
    viewHolder.telescopeLayout.setLens(new BugReportLens(activity, lumberYard));

    // If you have not seen the debug drawer before, show it with a message
    if (!seenDebugDrawer.get()) {
      viewHolder.drawerLayout.postDelayed(() -> {
        viewHolder.drawerLayout.openDrawer(GravityCompat.END);
        Toast.makeText(drawerContext, R.string.debug_drawer_welcome, Toast.LENGTH_LONG).show();
      }, 1000);
      seenDebugDrawer.set(true);
    }

    final CompositeSubscription subscriptions = new CompositeSubscription();
    setupMadge(viewHolder, subscriptions);
    setupScalpel(viewHolder, subscriptions);

    riseAndShine(activity);
    activity.lifecycle()
        .to(new ObservableScoper<>(activity))
        .subscribe(activityEvent -> {
          switch (activityEvent) {
            case DESTROY:
              unbinder.unbind();
              subscriptions.clear();
              break;
            default:
              // Noop
          }
        });
    return viewHolder.content;
  }

  private void setupMadge(final ViewHolder viewHolder, CompositeSubscription subscriptions) {
    subscriptions.add(pixelGridEnabled.asObservable().subscribe(enabled -> {
      viewHolder.madgeFrameLayout.setOverlayEnabled(enabled);
    }));
    subscriptions.add(pixelRatioEnabled.asObservable().subscribe(enabled -> {
      viewHolder.madgeFrameLayout.setOverlayRatioEnabled(enabled);
    }));
  }

  private void setupScalpel(final ViewHolder viewHolder, CompositeSubscription subscriptions) {
    subscriptions.add(scalpelEnabled.asObservable().subscribe(enabled -> {
      viewHolder.content.setLayerInteractionEnabled(enabled);
    }));
    subscriptions.add(scalpelWireframeEnabled.asObservable().subscribe(enabled -> {
      viewHolder.content.setDrawViews(!enabled);
    }));
  }

  static class ViewHolder {
    @BindView(R.id.debug_drawer_layout) DebugDrawerLayout drawerLayout;
    @BindView(R.id.debug_drawer) ViewGroup debugDrawer;
    @BindView(R.id.telescope_container) TelescopeLayout telescopeLayout;
    @BindView(R.id.madge_container) MadgeFrameLayout madgeFrameLayout;
    @BindView(R.id.debug_content) ScalpelFrameLayout content;
  }
}
