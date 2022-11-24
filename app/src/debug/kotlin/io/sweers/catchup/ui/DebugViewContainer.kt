/*
 * Copyright (C) 2019. Zac Sweers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.sweers.catchup.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Color
import android.graphics.Rect
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
import autodispose2.android.scope
import autodispose2.autoDispose
import com.getkeepsafe.taptargetview.TapTarget
import com.mattprecious.telescope.TelescopeLayout
import com.squareup.anvil.annotations.ContributesBinding
import dagger.Lazy
import dev.zacsweers.catchup.appconfig.AppConfig
import dev.zacsweers.catchup.di.AppScope
import dev.zacsweers.catchup.di.SingleIn
import io.reactivex.rxjava3.android.MainThreadDisposable
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.schedulers.Schedulers
import io.sweers.catchup.R
import io.sweers.catchup.base.ui.ActivityEvent
import io.sweers.catchup.base.ui.BaseActivity
import io.sweers.catchup.base.ui.ViewContainer
import io.sweers.catchup.data.DebugPreferences
import io.sweers.catchup.data.LumberYard
import io.sweers.catchup.databinding.DebugActivityFrameBinding
import io.sweers.catchup.edu.Syllabus
import io.sweers.catchup.edu.TargetRequest
import io.sweers.catchup.edu.id
import io.sweers.catchup.flowFor
import io.sweers.catchup.ui.bugreport.BugReportLens
import io.sweers.catchup.ui.debug.DebugView
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.SECONDS
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

/**
 * An [ViewContainer] for debug builds which wraps a sliding drawer on the right that holds all of
 * the debug information and settings.
 */
// TODO scope this better?
@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
class DebugViewContainer
@Inject
constructor(
  private val bugReportLensFactory: BugReportLens.Factory,
  private val lumberYard: LumberYard,
  private val lazyOkHttpClient: Lazy<OkHttpClient>,
  private val syllabus: Syllabus,
  private val fontHelper: FontHelper,
  private val debugPreferences: DebugPreferences,
  private val appConfig: AppConfig
) : ViewContainer {
  private val pixelGridEnabled = debugPreferences.flowFor { ::pixelGridEnabled }
  private val pixelRatioEnabled = debugPreferences.flowFor { ::pixelRatioEnabled }
  private val scalpelEnabled = debugPreferences.flowFor { ::scalpelEnabled }
  private val scalpelWireframeEnabled = debugPreferences.flowFor { ::scalpelWireframeDrawer }

  override fun forActivity(activity: BaseActivity): ViewGroup {
    val viewHolder =
      DebugActivityFrameBinding.inflate(
        LayoutInflater.from(activity),
        activity.findViewById(android.R.id.content),
        false
      )
    activity.setContentView(viewHolder.root)

    val drawerContext = ContextThemeWrapper(activity, R.style.DebugDrawer)
    val debugView =
      DebugView(drawerContext, null, lazyOkHttpClient, lumberYard, debugPreferences, appConfig)
    viewHolder.debugDrawer.addView(debugView)

    // Set up the contextual actions to watch views coming in and out of the content area.
    //    ContextualDebugActions contextualActions = debugView.getContextualDebugActions();
    //    contextualActions.setActionClickListener(v -> viewHolder.drawerLayout.closeDrawers());
    //    viewHolder.content.setOnHierarchyChangeListener(
    //        HierarchyTreeChangeListener.wrap(contextualActions));

    //    viewHolder.drawerLayout.setDrawerShadow(R.drawable.debug_drawer_shadow,
    // GravityCompat.END);
    viewHolder.drawerLayout.addDrawerListener(
      object : DrawerLayout.SimpleDrawerListener() {
        override fun onDrawerOpened(drawerView: View) {
          debugView.onDrawerOpened()
        }
      }
    )

    viewHolder.telescopeLayout.setPointerCount(3)
    val lens = bugReportLensFactory.create(activity)
    Completable.fromAction {
        TelescopeLayout.cleanUp(activity) // Clean up any old screenshots.
      }
      .subscribeOn(Schedulers.io())
      .observeOn(AndroidSchedulers.mainThread())
      .autoDispose(activity)
      .subscribe { viewHolder.telescopeLayout.setLens(lens) }

    // If you have not seen the debug drawer before, show it with a message
    syllabus.showIfNeverSeen(
      debugPreferences::seenDebugDrawer.name,
      TargetRequest(
        target = {
          DrawerTapTarget(
              delegateTarget = TapTarget.forView(debugView.icon, "", ""),
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
        postDisplay = { viewHolder.drawerLayout.closeDrawer(GravityCompat.END) }
      )
    )

    val scope =
      object : CoroutineScope {
        override val coroutineContext: CoroutineContext =
          SupervisorJob() + Dispatchers.Main.immediate
      }
    setupMadge(viewHolder, scope)
    setupScalpel(viewHolder, scope)

    riseAndShine(activity, appConfig)
    activity
      .lifecycle()
      .filter { event -> event === ActivityEvent.DESTROY }
      .firstElement()
      // Why is the below all so awkward?
      .doOnDispose { scope.cancel() }
      .autoDispose(activity)
      .subscribe { scope.cancel() }
    return viewHolder.debugContent
  }

  private fun setupMadge(viewHolder: DebugActivityFrameBinding, scope: CoroutineScope) =
    scope.launch {
      pixelGridEnabled.collect { enabled -> viewHolder.madgeContainer.isOverlayEnabled = enabled }
      pixelRatioEnabled.collect { enabled ->
        viewHolder.madgeContainer.isOverlayRatioEnabled = enabled
      }
    }

  private fun setupScalpel(viewHolder: DebugActivityFrameBinding, scope: CoroutineScope) =
    scope.launch {
      scalpelEnabled.collect { enabled ->
        viewHolder.debugContent.isLayerInteractionEnabled = enabled
      }
      scalpelWireframeEnabled.collect { enabled -> viewHolder.debugContent.setDrawViews(!enabled) }
    }

  companion object {

    /**
     * Show the activity over the lock-screen and wake up the device. If you launched the app
     * manually both of these conditions are already true. If you deployed from the IDE, however,
     * this will save you from hundreds of power button presses and pattern swiping per day!
     */
    @SuppressLint("NewApi") // False positive
    @Suppress("DEPRECATION")
    fun riseAndShine(activity: Activity, appConfig: AppConfig) {
      if (appConfig.sdkInt >= 27) {
        // Don't run on Q+ because the gesture nav makes this a crappy experience
        if (appConfig.sdkInt < 29) {
          activity.run {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
          }
        }
      } else {
        activity.window.addFlags(FLAG_SHOW_WHEN_LOCKED)
        activity.getSystemService<PowerManager>()?.run {
          newWakeLock(
              FULL_WAKE_LOCK or ACQUIRE_CAUSES_WAKEUP or ON_AFTER_RELEASE,
              "CatchUp:wakeup!"
            )
            .run {
              acquire(TimeUnit.MILLISECONDS.convert(1, SECONDS))
              release()
            }
        }
      }
    }
  }
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
            val listener =
              object : MainThreadDisposable(), DrawerListener {
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
          .autoDispose(drawerLayout.scope())
          .subscribe { delegateTarget.onReady(runnable) }

        drawerLayout.openDrawer(gravity)
      }
    }
  }
}
