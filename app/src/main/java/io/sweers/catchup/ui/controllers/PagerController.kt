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

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.support.annotation.ColorInt
import android.support.annotation.ColorRes
import android.support.annotation.DrawableRes
import android.support.annotation.StringRes
import android.support.design.widget.AppBarLayout
import android.support.design.widget.TabLayout
import android.support.graphics.drawable.VectorDrawableCompat
import android.support.v4.content.ContextCompat
import android.support.v4.view.ViewPager
import android.support.v4.view.animation.LinearOutSlowInInterpolator
import android.support.v7.app.AppCompatDelegate
import android.support.v7.widget.Toolbar
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import butterknife.BindView
import butterknife.Unbinder
import com.bluelinelabs.conductor.Controller
import com.bluelinelabs.conductor.Router
import com.bluelinelabs.conductor.RouterTransaction
import com.bluelinelabs.conductor.support.RouterPagerAdapter
import com.jakewharton.rxbinding2.support.design.widget.RxAppBarLayout
import com.uber.autodispose.kotlin.autoDisposeWith
import io.sweers.catchup.P
import io.sweers.catchup.R
import io.sweers.catchup.rx.PredicateConsumer
import io.sweers.catchup.ui.Scrollable
import io.sweers.catchup.ui.activity.SettingsActivity
import io.sweers.catchup.ui.base.ButterKnifeController
import io.sweers.catchup.util.clearLightStatusBar
import io.sweers.catchup.util.isInNightMode
import io.sweers.catchup.util.resolveAttribute
import io.sweers.catchup.util.setLightStatusBar
import java.util.Arrays

data class Service(@StringRes val name: Int, @DrawableRes val icon: Int, @ColorRes val accent: Int)

class PagerController : ButterKnifeController {

  companion object {

    private const val PAGE_TAG = "PagerController.pageTag"
    private val PAGE_DATA = arrayOf(
        Service(R.string.hacker_news, R.drawable.logo_hn, R.color.hackerNewsAccent),
        Service(R.string.reddit, R.drawable.logo_reddit, R.color.redditAccent),
        Service(R.string.medium, R.drawable.logo_medium, R.color.mediumAccent),
        Service(R.string.product_hunt, R.drawable.logo_ph, R.color.productHuntAccent),
        Service(R.string.slashdot, R.drawable.logo_sd, R.color.slashdotAccent),
        Service(R.string.designer_news, R.drawable.logo_dn, R.color.designerNewsAccent),
        Service(R.string.dribbble, R.drawable.logo_dribbble, R.color.dribbbleAccent),
        Service(R.string.github, R.drawable.logo_github, R.color.githubAccent))
  }

  private val resolvedColorCache = IntArray(PAGE_DATA.size)
  private val argbEvaluator = ArgbEvaluator()

  @BindView(R.id.tab_layout) lateinit var tabLayout: TabLayout
  @BindView(R.id.view_pager) lateinit var viewPager: ViewPager
  @BindView(R.id.toolbar) lateinit var toolbar: Toolbar
  @BindView(R.id.appbarlayout) lateinit var appBarLayout: AppBarLayout
  private var statusBarColorAnimator: ValueAnimator? = null
  private var colorNavBar = false
  private var tabLayoutIsPinned = false
  private var canAnimateColor = true
  private var lastPosition = 0
  private var pagerAdapter: RouterPagerAdapter

  constructor() : super()

  constructor(args: Bundle) : super(args)

  init {
    pagerAdapter = object : RouterPagerAdapter(this) {
      override fun configureRouter(router: Router, position: Int) {
        if (!router.hasRootController()) {
          val page: Controller = when (position) {
            0 -> HackerNewsController()
            1 -> RedditController()
            2 -> MediumController()
            3 -> ProductHuntController()
            4 -> SlashdotController()
            5 -> DesignerNewsController()
            6 -> DribbbleController()
            7 -> GitHubController()
            else -> RedditController()
          }
          router.setRoot(RouterTransaction.with(page)
              .tag(PAGE_TAG))
        }
      }

      override fun getCount(): Int {
        return PAGE_DATA.size
      }

      override fun getPageTitle(position: Int): CharSequence {
        return ""
      }
    }

    // Invalidate the color cache up front
    Arrays.fill(resolvedColorCache, R.color.no_color)
  }

  override fun inflateView(inflater: LayoutInflater, container: ViewGroup): View {
    return inflater.inflate(R.layout.controller_pager, container, false)
  }

  override fun bind(view: View): Unbinder {
    return PagerController_ViewBinding(this, view)
  }

  override fun onViewBound(view: View) {
    super.onViewBound(view)

    @ColorInt val colorPrimaryDark = view.context.resolveAttribute(R.attr.colorPrimaryDark)
    val isInNightMode = view.context.isInNightMode()
    if (!isInNightMode) {
      // Start with a light status bar in normal mode
      appBarLayout.setLightStatusBar()
    }
    RxAppBarLayout.offsetChanges(appBarLayout)
        .distinctUntilChanged()
        .doOnNext(object : PredicateConsumer<Int>() {
          @Throws(Exception::class)
          override fun test(verticalOffset: Int): Boolean {
            return verticalOffset == -toolbar.height
          }

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
          override fun test(verticalOffset: Int): Boolean {
            return verticalOffset != -toolbar.height
          }
        })
        .autoDisposeWith(this)
        .subscribe()
    toolbar.inflateMenu(R.menu.main)
    toolbar.setOnMenuItemClickListener { item ->
      when (item.itemId) {
        R.id.toggle_daynight -> {
          P.DaynightAuto.put(false)
              .commit()
          if (activity?.isInNightMode() ?: false) {
            P.DaynightNight.put(false)
                .commit()
          } else {
            P.DaynightNight.put(true)
                .commit()
          }
          // For whatever reason, the observable we set in CatchUpApplication on prefs doesn't
          // register the above changes, so manually do it here.
          var nightMode = AppCompatDelegate.MODE_NIGHT_NO
          if (P.DaynightAuto.get()) {
            nightMode = AppCompatDelegate.MODE_NIGHT_AUTO
          } else if (P.DaynightNight.get()) {
            nightMode = AppCompatDelegate.MODE_NIGHT_YES
          }
          AppCompatDelegate.setDefaultNightMode(nightMode)
          activity?.recreate()
          return@setOnMenuItemClickListener true
        }
        R.id.settings -> {
          startActivity(Intent(activity, SettingsActivity::class.java))
          return@setOnMenuItemClickListener true
        }
      }
      false
    }

    // Initial title
    toolbar.title = resources!!.getString(PAGE_DATA[0].name)

    // Set the initial color
    @ColorInt val initialColor = getAndSaveColor(0)
    tabLayout.setBackgroundColor(initialColor)
    viewPager.adapter = pagerAdapter
    tabLayout.setupWithViewPager(viewPager, false)

    // Set icons
    for (i in PAGE_DATA.indices) {
      val service = PAGE_DATA[i]
      val d = VectorDrawableCompat.create(resources!!, service.icon, null)
      tabLayout.getTabAt(i)!!.icon = d
    }

    // Animate color changes
    // adapted from http://kubaspatny.github.io/2014/09/18/viewpager-background-transition/
    viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
      override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
        if (canAnimateColor) {
          val color: Int
          if (position < pagerAdapter.count - 1 && position < PAGE_DATA.size - 1) {
            color = argbEvaluator.evaluate(positionOffset,
                getAndSaveColor(position),
                getAndSaveColor(position + 1)) as Int
          } else {
            color = getAndSaveColor(PAGE_DATA.size - 1)
          }
          tabLayout.setBackgroundColor(color)
          if (tabLayoutIsPinned) {
            activity?.window?.statusBarColor = color
          }
          if (colorNavBar) {
            activity?.window?.navigationBarColor = color
          }
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
        toolbar.setTitle(PAGE_DATA[position].name)

        // If we're switching between more than one page, we just want to manually set the color
        // once rather than let the usual page scroll logic cycle through all the colors in a weird
        // flashy way.
        if (Math.abs(lastPosition - position) > 1) {
          canAnimateColor = false
          val color = getAndSaveColor(position)
          tabLayout.setBackgroundColor(color)
          if (tabLayoutIsPinned) {
            activity?.window?.statusBarColor = color
          }
          if (colorNavBar) {
            activity?.window?.navigationBarColor = color
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

  @ColorInt private fun getAndSaveColor(position: Int): Int {
    if (resolvedColorCache[position] == R.color.no_color) {
      resolvedColorCache[position] = ContextCompat.getColor(activity!!, PAGE_DATA[position].accent)
    }
    return resolvedColorCache[position]
  }
}
