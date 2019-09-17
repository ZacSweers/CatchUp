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
package io.sweers.catchup.ui.activity

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commitNow
import androidx.recyclerview.widget.RecyclerView.RecycledViewPool
import com.uber.autodispose.autoDisposable
import dagger.Binds
import dagger.Provides
import dagger.multibindings.Multibinds
import io.sweers.catchup.R
import io.sweers.catchup.data.LinkManager
import io.sweers.catchup.data.ServiceDao
import io.sweers.catchup.edu.Syllabus
import io.sweers.catchup.injection.ActivityModule
import io.sweers.catchup.injection.scopes.PerActivity
import io.sweers.catchup.service.api.LinkHandler
import io.sweers.catchup.service.api.ScrollableContent
import io.sweers.catchup.service.api.Service
import io.sweers.catchup.service.api.ServiceMeta
import io.sweers.catchup.serviceregistry.ResolvedCatchUpServiceRegistry
import io.sweers.catchup.ui.DetailDisplayer
import io.sweers.catchup.base.ui.InjectingBaseActivity
import io.sweers.catchup.ui.fragments.PagerFragment
import io.sweers.catchup.ui.fragments.service.StorageBackedService
import io.sweers.catchup.util.customtabs.CustomTabActivityHelper
import kotterknife.bindView
import me.saket.inboxrecyclerview.InboxRecyclerView
import me.saket.inboxrecyclerview.dimming.TintPainter
import me.saket.inboxrecyclerview.page.ExpandablePageLayout
import me.saket.inboxrecyclerview.page.InterceptResult
import me.saket.inboxrecyclerview.page.SimplePageStateChangeCallbacks
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Qualifier

class MainActivity : InjectingBaseActivity() {

  @Inject
  internal lateinit var customTab: CustomTabActivityHelper
  @Inject
  internal lateinit var linkManager: LinkManager
  @Inject
  internal lateinit var syllabus: Syllabus
  @Inject
  internal lateinit var fragmentFactory: FragmentFactory

  internal val detailPage by bindView<ExpandablePageLayout>(R.id.detailPage)
  internal var pagerFragment: PagerFragment? = null

  override fun setFragmentFactory() {
    supportFragmentManager.fragmentFactory = fragmentFactory
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    testBadIntent(this)
    syllabus.bind(this)
    lifecycle()
        .doOnStart(linkManager) { connect(this@MainActivity) }
        .doOnStart(customTab) { bindCustomTabsService(this@MainActivity) }
        .doOnStop(customTab) { unbindCustomTabsService(this@MainActivity) }
        .doOnDestroy(customTab) { connectionCallback = null }
        .autoDisposable(this)
        .subscribe()

    val viewGroup = viewContainer.forActivity(this)
    layoutInflater.inflate(R.layout.activity_main, viewGroup)

    if (savedInstanceState == null) {
      supportFragmentManager.commitNow {
        add(R.id.fragment_container, PagerFragment().also { pagerFragment = it })
      }
    } else {
      pagerFragment = supportFragmentManager.findFragmentById(R.id.fragment_container) as PagerFragment
    }
  }

  @dagger.Module(includes = [ResolvedCatchUpServiceRegistry::class])
  abstract class ServiceIntegrationModule : ActivityModule<MainActivity> {
    @dagger.Module
    companion object {
      @TextViewPool
      @Provides
      @JvmStatic
      @PerActivity
      fun provideTextViewPool() = RecycledViewPool()

      @VisualViewPool
      @Provides
      @JvmStatic
      @PerActivity
      fun provideVisualViewPool() = RecycledViewPool()

      @Provides
      @PerActivity
      @JvmStatic
      @FinalServices
      fun provideFinalServices(
        serviceDao: ServiceDao,
        serviceMetas: Map<String, @JvmSuppressWildcards ServiceMeta>,
        sharedPreferences: SharedPreferences,
        services: Map<String, @JvmSuppressWildcards Provider<Service>>
      ): Map<String, Provider<Service>> {
        return services
            .filter {
              serviceMetas.getValue(it.key).enabled && sharedPreferences.getBoolean(
                  serviceMetas.getValue(it.key).enabledPreferenceKey, true)
            }
            .mapValues { (_, value) ->
              Provider { StorageBackedService(serviceDao, value.get()) }
            }
      }
    }

    @Multibinds
    abstract fun services(): Map<String, Service>

    @Multibinds
    abstract fun serviceMetas(): Map<String, ServiceMeta>

    @Multibinds
    abstract fun fragmentCreators(): Map<Class<out Fragment>, @JvmSuppressWildcards Fragment>

    @Binds
    @PerActivity
    abstract fun provideLinkHandler(linkManager: LinkManager): LinkHandler

    @Binds
    @PerActivity
    abstract fun provideDetailDisplayer(mainActivityDetailDisplayer: MainActivityDetailDisplayer): DetailDisplayer

    @Binds
    @PerActivity
    abstract fun provideFragmentFactory(fragmentFactory: MainActivityFragmentFactory): FragmentFactory
  }
}

@Qualifier
annotation class TextViewPool

@Qualifier
annotation class VisualViewPool

@Qualifier
annotation class FinalServices

/**
 * A displayer that repeatedly shows new detail views in a new ExpandablePageLayout.
 */
@PerActivity
class MainActivityDetailDisplayer @Inject constructor(
  private val mainActivity: MainActivity
) : DetailDisplayer {

  private var collapser: (() -> Unit)? = null

  private inline val detailPage get() = mainActivity.detailPage

  override val isExpandedOrExpanding get() = detailPage.isExpandedOrExpanding

  override fun showDetail(body: (ExpandablePageLayout, FragmentManager) -> () -> Unit) {
    collapser?.invoke()
    collapser = null
    detailPage.addStateChangeCallbacks(object : SimplePageStateChangeCallbacks() {
      override fun onPageCollapsed() {
        detailPage.removeStateChangeCallbacks(this)
        collapser = null
      }
    })
    collapser = body(detailPage, mainActivity.supportFragmentManager)
  }

  override fun bind(irv: InboxRecyclerView, useExistingFragment: Boolean) {
    val targetFragment = if (useExistingFragment) {
      mainActivity.supportFragmentManager.findFragmentById(detailPage.id)
    } else {
      null
    }
    bind(irv, targetFragment)
  }

  override fun bind(irv: InboxRecyclerView, targetFragment: Fragment?) {
    // TODO this is ugly, do better
    mainActivity.pagerFragment?.appBarLayout?.let {
      detailPage.pushParentToolbarOnExpand(it)
    }
    irv.expandablePage = detailPage
    irv.tintPainter = TintPainter.uncoveredArea(
        color = ContextCompat.getColor(irv.context, R.color.colorPrimary),
        opacity = 0.65F
    )
    if (targetFragment is ScrollableContent) {
      detailPage.pullToCollapseInterceptor = { _, _, upwardPull ->
        val directionInt = if (upwardPull) +1 else -1
        val canScrollFurther = targetFragment.canScrollVertically(directionInt)
        if (canScrollFurther) InterceptResult.INTERCEPTED else InterceptResult.IGNORED
      }
    }
  }

  override fun unbind(irv: InboxRecyclerView) {
    detailPage.pullToCollapseInterceptor = null
    irv.expandablePage = null
  }
}

fun testBadIntent(activity: Activity) {
  val intent = Intent(Intent.ACTION_SEND)
  intent.type = "text/plain"
  intent.putExtra(Intent.EXTRA_TEXT, crashContent)
  activity.startActivity(
      Intent.createChooser(intent, "Share with…")
  )
}

val crashContent = """
  ApplicationLeak(className=io.sweers.catchup.ui.fragments.service.ServiceFragment, leakTrace=
  ┬
  ├─ android.view.inputmethod.InputMethodManager
  │    Leaking: NO (InputMethodManager↓ is not leaking and a class is never leaking)
  │    GC Root: System class
  │    ↓ static InputMethodManager.sInstance
  ├─ android.view.inputmethod.InputMethodManager
  │    Leaking: NO (DecorView↓ is not leaking and InputMethodManager is a singleton)
  │    ↓ InputMethodManager.mNextServedView
  ├─ com.android.internal.policy.DecorView
  │    Leaking: NO (LinearLayout↓ is not leaking and View attached)
  │    mContext instance of com.android.internal.policy.DecorContext, wrapping activity io.sweers.catchup.ui.activity.MainActivity with mDestroyed = false
  │    Parent android.view.ViewRootImpl not a android.view.View
  │    View#mParent is set
  │    View#mAttachInfo is not null (view attached)
  │    View.mWindowAttachCount = 1
  │    ↓ DecorView.mContentRoot
  ├─ android.widget.LinearLayout
  │    Leaking: NO (MainActivity↓ is not leaking and View attached)
  │    mContext instance of io.sweers.catchup.ui.activity.MainActivity with mDestroyed = false
  │    View.parent com.android.internal.policy.DecorView attached as well
  │    View#mParent is set
  │    View#mAttachInfo is not null (view attached)
  │    View.mWindowAttachCount = 1
  │    ↓ LinearLayout.mContext
  ├─ io.sweers.catchup.ui.activity.MainActivity
  │    Leaking: NO (Activity#mDestroyed is false)
  │    ↓ MainActivity.mActivityTransitionState
  │                   ~~~~~~~~~~~~~~~~~~~~~~~~
  ├─ android.app.ActivityTransitionState
  │    Leaking: UNKNOWN
  │    ↓ ActivityTransitionState.mExitingToView
  │                              ~~~~~~~~~~~~~~
  ├─ java.util.ArrayList
  │    Leaking: UNKNOWN
  │    ↓ ArrayList.elementData
  │                ~~~~~~~~~~~
  ├─ java.lang.Object[]
  │    Leaking: UNKNOWN
  │    ↓ array Object[].[1]
  │                     ~~~
  ├─ io.sweers.catchup.ui.widget.BadgedFourThreeImageView
  │    Leaking: YES (View detached and has parent)
  │    mContext instance of io.sweers.catchup.ui.activity.MainActivity with mDestroyed = false
  │    View#mParent is set
  │    View#mAttachInfo is null (view detached)
  │    View.mWindowAttachCount = 16
  │    ↓ BadgedFourThreeImageView.mParent
  ├─ me.saket.inboxrecyclerview.InboxRecyclerView
  │    Leaking: YES (BadgedFourThreeImageView↑ is leaking and View detached and has parent)
  │    mContext instance of io.sweers.catchup.ui.activity.MainActivity with mDestroyed = false
  │    View#mParent is set
  │    View#mAttachInfo is null (view detached)
  │    View.mWindowAttachCount = 1
  │    ↓ InboxRecyclerView.mParent
  ├─ androidx.swiperefreshlayout.widget.SwipeRefreshLayout
  │    Leaking: YES (InboxRecyclerView↑ is leaking and View detached and has parent)
  │    mContext instance of io.sweers.catchup.ui.activity.MainActivity with mDestroyed = false
  │    View#mParent is set
  │    View#mAttachInfo is null (view detached)
  │    View.mWindowAttachCount = 1
  │    ↓ SwipeRefreshLayout.mListener
  ╰→ io.sweers.catchup.ui.fragments.service.ServiceFragment
  ​     Leaking: YES (SwipeRefreshLayout↑ is leaking and Fragment#mFragmentManager is null and ObjectWatcher was watching this)
  ​     key = e2d6bd34-aa06-409d-a760-97137f9c285d
  ​     watchDurationMillis = 302438
  ​     retainedDurationMillis = 297437
  ​     key = 3844c1b4-4249-4c80-acdc-f6cbd5ccc9be
  , retainedHeapByteSize=3433)
""".trimIndent()
