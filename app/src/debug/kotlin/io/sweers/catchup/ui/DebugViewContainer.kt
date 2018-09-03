/*
 * Copyright (c) 2018 Zac Sweers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.sweers.catchup.ui

import android.app.Activity
import android.graphics.Color
import android.graphics.Rect
import android.os.Build
import android.os.PowerManager
import android.os.PowerManager.ACQUIRE_CAUSES_WAKEUP
import android.os.PowerManager.FULL_WAKE_LOCK
import android.os.PowerManager.ON_AFTER_RELEASE
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
import androidx.core.content.getSystemService
import androidx.core.view.GravityCompat
import androidx.core.view.doOnLayout
import androidx.drawerlayout.widget.DrawerLayout
import androidx.drawerlayout.widget.DrawerLayout.DrawerListener
import com.getkeepsafe.taptargetview.TapTarget
import com.jakewharton.madge.MadgeFrameLayout
import com.jakewharton.scalpel.ScalpelFrameLayout
import com.mattprecious.telescope.TelescopeLayout
import com.uber.autodispose.android.scope
import com.uber.autodispose.autoDisposable
import dagger.Lazy
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.android.MainThreadDisposable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.sweers.catchup.P
import io.sweers.catchup.R
import io.sweers.catchup.data.LumberYard
import io.sweers.catchup.edu.Syllabus
import io.sweers.catchup.edu.TargetRequest
import io.sweers.catchup.edu.id
import io.sweers.catchup.injection.scopes.PerActivity
import io.sweers.catchup.ui.base.ActivityEvent
import io.sweers.catchup.ui.base.BaseActivity
import io.sweers.catchup.ui.bugreport.BugReportLens
import io.sweers.catchup.ui.debug.DebugView
import kotterknife.ViewDelegateBindable
import kotterknife.bindView
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.SECONDS
import javax.inject.Inject

/**
 * An [ViewContainer] for debug builds which wraps a sliding drawer on the right that holds
 * all of the debug information and settings.
 */
@PerActivity
internal class DebugViewContainer @Inject constructor(
    private val bugReportLens: BugReportLens,
    private val lumberYard: LumberYard,
    private val lazyOkHttpClient: Lazy<OkHttpClient>,
    private val syllabus: Syllabus,
    private val fontHelper: FontHelper) : ViewContainer {
  private val seenDebugDrawer = P.DebugSeenDebugDrawer.rx()
  private val pixelGridEnabled = P.DebugPixelGridEnabled.rx()
  private val pixelRatioEnabled = P.DebugPixelRatioEnabled.rx()
  private val scalpelEnabled = P.DebugScalpelEnabled.rx()
  private val scalpelWireframeEnabled = P.DebugScalpelWireframeDrawer.rx()

  override fun forActivity(activity: BaseActivity): ViewGroup {
    val contentView = LayoutInflater.from(activity)
        .inflate(R.layout.debug_activity_frame,
            activity.findViewById(android.R.id.content), false)
    activity.setContentView(contentView)

    val viewHolder = DebugViewViewHolder(contentView)

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
    Completable
        .fromAction {
          TelescopeLayout.cleanUp(activity) // Clean up any old screenshots.
        }
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .autoDisposable(activity)
        .subscribe {
          viewHolder.telescopeLayout.setLens(bugReportLens)
        }

    // If you have not seen the debug drawer before, show it with a message
    syllabus.showIfNeverSeen(seenDebugDrawer.key(), TargetRequest(
        target = {
          DrawerTapTarget(
              delegateTarget = TapTarget.forView(debugView.icon,
                  "",
                  ""),
              drawerLayout = viewHolder.drawerLayout,
              gravity = Gravity.END,
              title = debugView.resources.getString(R.string.development_settings),
              description = debugView.resources.getString(R.string.debug_drawer_welcome)
          )
              .outerCircleColorInt(Color.parseColor("#EE222222"))
              .outerCircleAlpha(0.96f)
              .titleTextColorInt(Color.WHITE)
              .descriptionTextColorInt(Color.parseColor("#33FFFFFF"))
              .targetCircleColorInt(Color.WHITE)
              .drawShadow(true)
              .transparentTarget(true)
              .id("DebugDrawer")
              .apply { fontHelper.getFont()?.let(::textTypeface) }
        },
        postDisplay = {
          viewHolder.drawerLayout.closeDrawer(GravityCompat.END)
        }
    ))

    val disposables = CompositeDisposable()
    setupMadge(viewHolder, disposables)
    setupScalpel(viewHolder, disposables)

    riseAndShine(activity)
    activity.lifecycle()
        .filter { event -> event === ActivityEvent.DESTROY }
        .firstElement()
        // Why is the below all so awkward?
        .doOnDispose {
          disposables.clear()
        }
        .autoDisposable(activity)
        .subscribe {
          disposables.clear()
        }
    return viewHolder.content
  }

  private fun setupMadge(viewHolder: DebugViewViewHolder, subscriptions: CompositeDisposable) {
    subscriptions.add(pixelGridEnabled.asObservable()
        .subscribe { enabled -> viewHolder.madgeFrameLayout.isOverlayEnabled = enabled })
    subscriptions.add(pixelRatioEnabled.asObservable()
        .subscribe { enabled -> viewHolder.madgeFrameLayout.isOverlayRatioEnabled = enabled })
  }

  private fun setupScalpel(viewHolder: DebugViewViewHolder, subscriptions: CompositeDisposable) {
    subscriptions.add(scalpelEnabled.asObservable()
        .subscribe { enabled -> viewHolder.content.isLayerInteractionEnabled = enabled })
    subscriptions.add(scalpelWireframeEnabled.asObservable()
        .subscribe { enabled -> viewHolder.content.setDrawViews(!enabled) })
  }

  companion object {

    /**
     * Show the activity over the lock-screen and wake up the device. If you launched the app manually
     * both of these conditions are already true. If you deployed from the IDE, however, this will
     * save you from hundreds of power button presses and pattern swiping per day!
     */
    @Suppress("DEPRECATION")
    fun riseAndShine(activity: Activity) {
      if (Build.VERSION.SDK_INT >= 27) {
        activity.run {
          setShowWhenLocked(true)
          setTurnScreenOn(true)
        }
      } else {
        activity.window
            .addFlags(FLAG_SHOW_WHEN_LOCKED)
        activity.getSystemService<PowerManager>()?.run {
          newWakeLock(FULL_WAKE_LOCK or ACQUIRE_CAUSES_WAKEUP or ON_AFTER_RELEASE,
              "CatchUp:wakeup!").run {
            acquire(TimeUnit.MILLISECONDS.convert(1, SECONDS))
            release()
          }
        }
      }
    }
  }
}

internal class DebugViewViewHolder(source: View) : ViewDelegateBindable(source) {
  val drawerLayout by bindView<DrawerLayout>(R.id.debug_drawer_layout)
  val debugDrawer by bindView<ViewGroup>(R.id.debug_drawer)
  val telescopeLayout by bindView<TelescopeLayout>(R.id.telescope_container)
  val madgeFrameLayout by bindView<MadgeFrameLayout>(R.id.madge_container)
  val content by bindView<ScalpelFrameLayout>(R.id.debug_content)
}

class DrawerTapTarget(
    private val delegateTarget: TapTarget,
    private val drawerLayout: DrawerLayout,
    private val gravity: Int,
    title: CharSequence,
    description: CharSequence?
) : TapTarget(title, description) {

  override fun bounds(): Rect {
    return delegateTarget.bounds()
  }

  override fun onReady(runnable: Runnable) {
    // Use `it` eventually: https://github.com/android/android-ktx/pull/177
    drawerLayout.doOnLayout {
      if (drawerLayout.isDrawerOpen(gravity)) {
        delegateTarget.onReady(runnable)
      } else {
        // TODO Jetifier bug. Want to use RxBinding here
        Maybe.create<Unit> { e ->
          val listener = object : MainThreadDisposable(), DrawerListener {
            override fun onDrawerStateChanged(newState: Int) {
              // Noop
            }

            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
              // Noop
            }

            override fun onDrawerClosed(drawerView: View) {
              // Noop
            }

            override fun onDispose() {
              drawerLayout.removeDrawerListener(this)
            }

            override fun onDrawerOpened(drawerView: View) {
              if (!isDisposed) {
                e.onSuccess(Unit)
              }
            }
          }
          e.setDisposable(listener)
          drawerLayout.addDrawerListener(listener)
        }
            .autoDisposable(drawerLayout.scope())
            .subscribe {
              delegateTarget.onReady(runnable)
            }

        drawerLayout.openDrawer(gravity)
      }
    }
  }
}
