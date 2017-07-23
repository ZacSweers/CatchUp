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

package io.sweers.catchup.ui

import android.app.Activity
import android.content.Context.POWER_SERVICE
import android.os.PowerManager
import android.os.PowerManager.ACQUIRE_CAUSES_WAKEUP
import android.os.PowerManager.FULL_WAKE_LOCK
import android.os.PowerManager.ON_AFTER_RELEASE
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
import android.widget.Toast
import butterknife.BindView
import com.jakewharton.madge.MadgeFrameLayout
import com.jakewharton.scalpel.ScalpelFrameLayout
import com.mattprecious.telescope.TelescopeLayout
import com.uber.autodispose.kotlin.autoDisposeWith
import dagger.Lazy
import io.reactivex.disposables.CompositeDisposable
import io.sweers.catchup.P
import io.sweers.catchup.R
import io.sweers.catchup.data.LumberYard
import io.sweers.catchup.injection.scopes.PerActivity
import io.sweers.catchup.ui.base.ActivityEvent
import io.sweers.catchup.ui.base.BaseActivity
import io.sweers.catchup.ui.debug.DebugView
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.SECONDS
import javax.inject.Inject

/**
 * An [ViewContainer] for debug builds which wraps a sliding drawer on the right that holds
 * all of the debug information and settings.
 */
@PerActivity
class DebugViewContainer @Inject constructor(
    private val lumberYard: LumberYard,
    private val lazyOkHttpClient: Lazy<OkHttpClient>) : ViewContainer {
  private val seenDebugDrawer = P.DebugSeenDebugDrawer.rx()
  private val pixelGridEnabled = P.DebugPixelGridEnabled.rx()
  private val pixelRatioEnabled = P.DebugPixelRatioEnabled.rx()
  private val scalpelEnabled = P.DebugScalpelEnabled.rx()
  private val scalpelWireframeEnabled = P.DebugScalpelWireframeDrawer.rx()

  override fun forActivity(activity: BaseActivity): ViewGroup {
    val contentView = LayoutInflater.from(activity)
        .inflate(R.layout.debug_activity_frame,
            activity.findViewById<ViewGroup>(android.R.id.content), false)
    activity.setContentView(contentView)

    val viewHolder = ViewHolder()
    val unbinder = `DebugViewContainer$ViewHolder_ViewBinding`(viewHolder, contentView)

    val drawerContext = ContextThemeWrapper(activity, R.style.DebugDrawer)
    val debugView = DebugView(drawerContext, lazyOkHttpClient, lumberYard)
    viewHolder.debugDrawer.addView(debugView)

    // Set up the contextual actions to watch views coming in and out of the content area.
    //    ContextualDebugActions contextualActions = debugView.getContextualDebugActions();
    //    contextualActions.setActionClickListener(v -> viewHolder.drawerLayout.closeDrawers());
    //    viewHolder.content.setOnHierarchyChangeListener(
    //        HierarchyTreeChangeListener.wrap(contextualActions));

    //    viewHolder.drawerLayout.setDrawerShadow(R.drawable.debug_drawer_shadow, GravityCompat.END);
    viewHolder.drawerLayout.addDrawerListener(object : DrawerLayout.SimpleDrawerListener() {
      override fun onDrawerOpened(drawerView: View) {
        debugView.onDrawerOpened()
      }
    })

    viewHolder.telescopeLayout.setPointerCount(3)
    TelescopeLayout.cleanUp(activity) // Clean up any old screenshots.
    viewHolder.telescopeLayout.setLens(BugReportLens(activity, lumberYard))

    // If you have not seen the debug drawer before, show it with a message
    if (!(seenDebugDrawer.get())) {
      viewHolder.drawerLayout.postDelayed({
        viewHolder.drawerLayout.openDrawer(GravityCompat.END)
        Toast.makeText(drawerContext, R.string.debug_drawer_welcome, Toast.LENGTH_LONG)
            .show()
      }, 1000)
      seenDebugDrawer.set(true)
    }

    val disposables = CompositeDisposable()
    setupMadge(viewHolder, disposables)
    setupScalpel(viewHolder, disposables)

    riseAndShine(activity)
    activity.lifecycle()
        .filter { event -> event === ActivityEvent.DESTROY }
        .firstElement()
        .autoDisposeWith(activity)
        .subscribe {
          unbinder.unbind()
          disposables.clear()
        }
    return viewHolder.content
  }

  private fun setupMadge(viewHolder: ViewHolder, subscriptions: CompositeDisposable) {
    subscriptions.add(pixelGridEnabled.asObservable()
        .subscribe { enabled -> viewHolder.madgeFrameLayout.isOverlayEnabled = enabled })
    subscriptions.add(pixelRatioEnabled.asObservable()
        .subscribe { enabled -> viewHolder.madgeFrameLayout.isOverlayRatioEnabled = enabled })
  }

  private fun setupScalpel(viewHolder: ViewHolder, subscriptions: CompositeDisposable) {
    subscriptions.add(scalpelEnabled.asObservable()
        .subscribe { enabled -> viewHolder.content.isLayerInteractionEnabled = enabled })
    subscriptions.add(scalpelWireframeEnabled.asObservable()
        .subscribe { enabled -> viewHolder.content.setDrawViews(!enabled) })
  }

  internal class ViewHolder {
    @BindView(R.id.debug_drawer_layout) lateinit var drawerLayout: DrawerLayout
    @BindView(R.id.debug_drawer) lateinit var debugDrawer: ViewGroup
    @BindView(R.id.telescope_container) lateinit var telescopeLayout: TelescopeLayout
    @BindView(R.id.madge_container) lateinit var madgeFrameLayout: MadgeFrameLayout
    @BindView(R.id.debug_content) lateinit var content: ScalpelFrameLayout
  }

  companion object {

    /**
     * Show the activity over the lock-screen and wake up the device. If you launched the app manually
     * both of these conditions are already true. If you deployed from the IDE, however, this will
     * save you from hundreds of power button presses and pattern swiping per day!
     */
    fun riseAndShine(activity: Activity) {
      activity.window
          .addFlags(FLAG_SHOW_WHEN_LOCKED)

      val power = activity.getSystemService(POWER_SERVICE) as PowerManager
      @Suppress("DEPRECATION")
      power.newWakeLock(FULL_WAKE_LOCK or ACQUIRE_CAUSES_WAKEUP or ON_AFTER_RELEASE,
          "wakeup!").run {
        acquire(TimeUnit.MILLISECONDS.convert(1, SECONDS))
        release()
      }
    }
  }
}
