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

package io.sweers.catchup.ui.controllers

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Parcelable
import android.support.annotation.ColorInt
import android.support.annotation.ColorRes
import android.support.annotation.DrawableRes
import android.support.annotation.StringRes
import android.support.design.widget.AppBarLayout
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.TabLayout
import android.support.graphics.drawable.VectorDrawableCompat
import android.support.v4.content.ContextCompat
import android.support.v4.view.ViewPager
import android.support.v4.view.animation.FastOutSlowInInterpolator
import android.support.v4.view.animation.LinearOutSlowInInterpolator
import android.support.v7.widget.Toolbar
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import butterknife.BindView
import com.bluelinelabs.conductor.Controller
import com.bluelinelabs.conductor.Router
import com.bluelinelabs.conductor.RouterTransaction
import com.bluelinelabs.conductor.support.RouterPagerAdapter
import com.jakewharton.rxbinding2.support.design.widget.RxAppBarLayout
import com.uber.autodispose.kotlin.autoDisposable
import dagger.Provides
import dagger.Subcomponent
import dagger.android.AndroidInjector
import io.sweers.catchup.P
import io.sweers.catchup.R
import io.sweers.catchup.changes.ChangelogHelper
import io.sweers.catchup.injection.ConductorInjection
import io.sweers.catchup.injection.scopes.PerController
import io.sweers.catchup.rx.PredicateConsumer
import io.sweers.catchup.service.api.ServiceMeta
import io.sweers.catchup.ui.Scrollable
import io.sweers.catchup.ui.activity.SettingsActivity
import io.sweers.catchup.ui.base.ButterKnifeController
import io.sweers.catchup.ui.controllers.service.ServiceController
import io.sweers.catchup.util.clearLightStatusBar
import io.sweers.catchup.util.isInNightMode
import io.sweers.catchup.util.resolveAttributeColor
import io.sweers.catchup.util.setLightStatusBar
import io.sweers.catchup.util.updateNavBarColor
import javax.inject.Inject

fun ServiceMeta.toServiceHandler() = ServiceHandler(
    name,
    icon,
    themeColor,
    { ServiceController.newInstance(id) }
)

data class ServiceHandler(@StringRes val name: Int,
    @DrawableRes val icon: Int,
    @ColorRes val accent: Int,
    val instantiator: () -> Controller)

class PagerController : ButterKnifeController {

  companion object {

    private const val SETTINGS_ACTIVITY_REQUEST = 100
    private const val PAGE_TAG = "PagerController.pageTag"
  }

  @Inject
  lateinit var resolvedColorCache: IntArray
  @Inject
  lateinit var serviceHandlers: Array<ServiceHandler>
  @Inject
  lateinit var changelogHelper: ChangelogHelper

  @BindView(R.id.pager_controller_root)
  lateinit var rootLayout: CoordinatorLayout
  @BindView(R.id.tab_layout)
  lateinit var tabLayout: TabLayout
  @BindView(R.id.view_pager)
  lateinit var viewPager: ViewPager
  @BindView(R.id.toolbar)
  lateinit var toolbar: Toolbar
  @BindView(R.id.appbarlayout)
  lateinit var appBarLayout: AppBarLayout

  // Not injectable because I don't know how to get a @BindsInstance into dagger's AndroidInjector
  // and this needs a controller to be instantiated
  private lateinit var pagerAdapter: RouterPagerAdapter

  private val argbEvaluator = ArgbEvaluator()
  private var statusBarColorAnimator: ValueAnimator? = null
  private var tabLayoutColorAnimator: Animator? = null
  private var tabLayoutIsPinned = false
  private var canAnimateColor = true
  private var lastPosition = 0

  constructor() : super()

  @Suppress("unused")
  constructor(args: Bundle) : super(args)

  override fun onContextAvailable(context: Context) {
    ConductorInjection.inject(this)
    super.onContextAvailable(context)

    pagerAdapter = object : RouterPagerAdapter(this) {
      override fun configureRouter(router: Router, position: Int) {
        if (!router.hasRootController()) {
          router.setRoot(RouterTransaction.with(serviceHandlers[position].instantiator())
              .tag("$PAGE_TAG.${serviceHandlers[position].name}"))
        }
      }

      override fun getCount() = serviceHandlers.size

      override fun getPageTitle(position: Int) = ""
    }
  }

  override fun onSaveViewState(view: View, outState: Bundle) {
    // Save the appbarlayout state to restore it on the other side
    (appBarLayout.layoutParams as CoordinatorLayout.LayoutParams).behavior?.let { behavior ->
      outState.run {
        putParcelable("collapsingToolbarState",
            behavior.onSaveInstanceState(rootLayout, appBarLayout))
      }
    }
    super.onSaveViewState(view, outState)
  }

  override fun onRestoreViewState(view: View, savedViewState: Bundle) {
    super.onRestoreViewState(view, savedViewState)
    with(savedViewState) {
      getParcelable<Parcelable>("collapsingToolbarState")?.let {
        (appBarLayout.layoutParams as CoordinatorLayout.LayoutParams).behavior
            ?.onRestoreInstanceState(rootLayout, appBarLayout, it)
      }
    }
  }

  override fun inflateView(inflater: LayoutInflater, container: ViewGroup): View =
      inflater.inflate(R.layout.controller_pager, container, false)

  override fun bind(view: View) = PagerController_ViewBinding(this, view)

  override fun onAttach(view: View) {
    ConductorInjection.inject(this)
    super.onAttach(view)
  }

  override fun onViewBound(view: View) {
    super.onViewBound(view)

    @ColorInt val colorPrimaryDark = view.context.resolveAttributeColor(R.attr.colorPrimaryDark)
    val isInNightMode = view.context.isInNightMode()
    if (!isInNightMode) {
      // Start with a light status bar in normal mode
      appBarLayout.setLightStatusBar()
    }
    RxAppBarLayout.offsetChanges(appBarLayout)
        .distinctUntilChanged()
        .doOnNext(object : PredicateConsumer<Int>() {
          @Throws(Exception::class)
          override fun test(verticalOffset: Int) = verticalOffset == -toolbar.height

          @Throws(Exception::class)
          override fun acceptActual(value: Int) {
            statusBarColorAnimator?.cancel()
            tabLayoutIsPinned = true
            val newStatusColor = this@PagerController.getAndSaveColor(
                tabLayout.selectedTabPosition)
            statusBarColorAnimator = ValueAnimator.ofArgb(colorPrimaryDark, newStatusColor)
                .apply {
                  addUpdateListener { animation ->
                    this@PagerController.activity!!
                        .window.statusBarColor = animation.animatedValue as Int
                  }
                  duration = 200
                  interpolator = LinearOutSlowInInterpolator()
                  start()
                }
            appBarLayout.clearLightStatusBar()
          }
        })
        .doOnNext(object : PredicateConsumer<Int>() {
          @Throws(Exception::class)
          override fun acceptActual(value: Int) {
            val wasPinned = tabLayoutIsPinned
            tabLayoutIsPinned = false
            if (wasPinned) {
              statusBarColorAnimator?.cancel()
              statusBarColorAnimator = ValueAnimator.ofArgb(this@PagerController.activity!!
                  .window
                  .statusBarColor, colorPrimaryDark).apply {
                addUpdateListener { animation ->
                  this@PagerController.activity!!
                      .window.statusBarColor = animation.animatedValue as Int
                }
                duration = 200
                interpolator = DecelerateInterpolator()
                if (!isInNightMode) {
                  appBarLayout.setLightStatusBar()
                }
                start()
              }
            }
          }

          @Throws(Exception::class)
          override fun test(verticalOffset: Int) = verticalOffset != -toolbar.height
        })
        .autoDisposable(this)
        .subscribe()
    toolbar.inflateMenu(R.menu.main)
    toolbar.setOnMenuItemClickListener { item ->
      when (item.itemId) {
        R.id.settings -> {
          startActivityForResult(
              Intent(activity, SettingsActivity::class.java), SETTINGS_ACTIVITY_REQUEST)
          return@setOnMenuItemClickListener true
        }
      }
      false
    }

    // Initial title
    toolbar.title = resources!!.getString(serviceHandlers[0].name)

    // Set the initial color
    @ColorInt val initialColor = getAndSaveColor(0)
    tabLayout.setBackgroundColor(initialColor)
    viewPager.adapter = pagerAdapter
    tabLayout.setupWithViewPager(viewPager, false)
    changelogHelper.bindWith(toolbar, initialColor) {
      getAndSaveColor(tabLayout.selectedTabPosition)
    }

    // Set icons
    for (i in serviceHandlers.indices) {
      val service = serviceHandlers[i]
      val d = VectorDrawableCompat.create(resources!!, service.icon, null)
      tabLayout.getTabAt(i)!!.icon = d
    }

    // Animate color changes
    // adapted from http://kubaspatny.github.io/2014/09/18/viewpager-background-transition/
    viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
      override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
        if (canAnimateColor) {
          val color: Int = if (position < pagerAdapter.count - 1 && position < serviceHandlers.size - 1) {
            argbEvaluator.evaluate(positionOffset,
                getAndSaveColor(position),
                getAndSaveColor(position + 1)) as Int
          } else {
            getAndSaveColor(serviceHandlers.size - 1)
          }
          tabLayout.setBackgroundColor(color)
          if (tabLayoutIsPinned) {
            activity?.window?.statusBarColor = color
          }
          activity?.updateNavBarColor(color,
              context = view.context)
        }
      }

      override fun onPageSelected(position: Int) {}

      override fun onPageScrollStateChanged(state: Int) {
        if (state == ViewPager.SCROLL_STATE_IDLE) {
          canAnimateColor = true
        }
      }
    })

    tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
      override fun onTabSelected(tab: TabLayout.Tab) {
        val position = tab.position
        toolbar.setTitle(serviceHandlers[position].name)

        // If we're switching between more than one page, we just want to manually set the color
        // once rather than let the usual page scroll logic cycle through all the colors in a weird
        // flashy way.
        if (Math.abs(lastPosition - position) > 1) {
          canAnimateColor = false
          // Start with the current tablayout color to feel more natural if we're in between
          @ColorInt val startColor = (tabLayout.background as ColorDrawable).color
          @ColorInt val endColor = getAndSaveColor(position)
          tabLayoutColorAnimator?.cancel()
          ValueAnimator.ofFloat(0f, 1f)
              .run {
                interpolator = FastOutSlowInInterpolator()  // TODO Use singleton
                duration = 400
                addListener(object : AnimatorListenerAdapter() {
                  override fun onAnimationStart(animator: Animator, isReverse: Boolean) {
                    tabLayoutColorAnimator = animator
                  }

                  override fun onAnimationEnd(animator: Animator) {
                    removeAllUpdateListeners()
                    removeListener(this)
                    tabLayoutColorAnimator = null
                  }
                })
                addUpdateListener { animator ->
                  @ColorInt val color = argbEvaluator.evaluate(animator.animatedValue as Float,
                      startColor,
                      endColor) as Int
                  tabLayout.setBackgroundColor(color)
                  if (tabLayoutIsPinned) {
                    activity?.window?.statusBarColor = color
                  }
                  activity?.updateNavBarColor(color,
                      context = view.context)
                }
                start()
              }
        }
        lastPosition = position
      }

      override fun onTabUnselected(tab: TabLayout.Tab) {}

      override fun onTabReselected(tab: TabLayout.Tab) {
        pagerAdapter.getRouter(tab.position)?.getControllerWithTag(PAGE_TAG)?.let {
              if (it is Scrollable) {
                it.onRequestScrollToTop()
                appBarLayout.setExpanded(true, true)
              }
            }
      }
    })
  }

  @ColorInt
  private fun getAndSaveColor(position: Int): Int {
    if (resolvedColorCache[position] == R.color.no_color) {
      resolvedColorCache[position] = ContextCompat.getColor(dayOnlyContext!!,
          serviceHandlers[position].accent)
    }
    return resolvedColorCache[position]
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    if (requestCode == SETTINGS_ACTIVITY_REQUEST) {
      if (resultCode == SettingsActivity.SETTINGS_RESULT_DATA && data != null) {
        val extras = data.extras
        if (extras.getBoolean(SettingsActivity.NIGHT_MODE_UPDATED, false)
            || extras.getBoolean(SettingsActivity.SERVICE_ORDER_UPDATED, false)) {
          for (i in 0 until tabLayout.tabCount) {
            pagerAdapter.getRouter(i)?.setBackstack(emptyList(), null)
          }
          activity?.recreate()
        }
        if (extras.getBoolean(SettingsActivity.NAV_COLOR_UPDATED, false)) {
          activity?.updateNavBarColor(color = (tabLayout.background as ColorDrawable).color,
              context = view!!.context,
              recreate = true)
        }
      }
    }
  }

  @PerController
  @Subcomponent(modules = [Module::class])
  interface Component : AndroidInjector<PagerController> {

    @Subcomponent.Builder
    abstract class Builder : AndroidInjector.Builder<PagerController>()
  }

  @dagger.Module
  object Module {

    @JvmStatic
    @Provides
    fun provideServiceHandlers(sharedPrefs: SharedPreferences,
        serviceMetas: Map<String, @JvmSuppressWildcards ServiceMeta>): Array<ServiceHandler> {
      val currentOrder = sharedPrefs.getString(P.ServicesOrder.KEY, null)?.split(",") ?: emptyList()
      return (serviceMetas.values
          .sortedBy { currentOrder.indexOf(it.id) }
          .map { it.toServiceHandler() })
          .toTypedArray()
    }

    @JvmStatic
    @Provides
    fun provideColorCache(serviceHandlers: Array<ServiceHandler>) = IntArray(
        serviceHandlers.size).apply { fill(R.color.no_color) }
  }
}
