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

package io.sweers.catchup.ui.about

import `in`.uncod.android.bypass.Bypass
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.os.Parcelable
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.Px
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.util.layoutDirection
import androidx.viewpager.widget.ViewPager
import butterknife.BindView
import butterknife.ButterKnife
import com.bluelinelabs.conductor.Conductor
import com.bluelinelabs.conductor.Controller
import com.bluelinelabs.conductor.Router
import com.bluelinelabs.conductor.RouterTransaction
import com.bluelinelabs.conductor.support.RouterPagerAdapter
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.tabs.TabLayout
import com.jakewharton.rxbinding2.support.design.widget.RxAppBarLayout
import com.uber.autodispose.kotlin.autoDisposable
import dagger.Binds
import dagger.Module
import dagger.Subcomponent
import dagger.android.AndroidInjector
import dagger.multibindings.IntoMap
import io.sweers.catchup.BuildConfig
import io.sweers.catchup.R
import io.sweers.catchup.data.LinkManager
import io.sweers.catchup.injection.ConductorInjection
import io.sweers.catchup.injection.ControllerKey
import io.sweers.catchup.injection.scopes.PerActivity
import io.sweers.catchup.injection.scopes.PerController
import io.sweers.catchup.service.api.UrlMeta
import io.sweers.catchup.ui.Scrollable
import io.sweers.catchup.ui.base.BaseActivity
import io.sweers.catchup.ui.base.ButterKnifeController
import io.sweers.catchup.util.LinkTouchMovementMethod
import io.sweers.catchup.util.TouchableUrlSpan
import io.sweers.catchup.util.UiUtil
import io.sweers.catchup.util.buildMarkdown
import io.sweers.catchup.util.customtabs.CustomTabActivityHelper
import io.sweers.catchup.util.isInNightMode
import io.sweers.catchup.util.parseMarkdownAndPlainLinks
import io.sweers.catchup.util.setLightStatusBar
import java.util.Locale
import javax.inject.Inject

class AboutActivity : BaseActivity() {

  @Inject
  internal lateinit var customTab: CustomTabActivityHelper
  @Inject
  internal lateinit var linkManager: LinkManager
  @BindView(R.id.controller_container)
  internal lateinit var container: ViewGroup

  private lateinit var router: Router

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    lifecycle()
        .doOnStart(customTab) { bindCustomTabsService(this@AboutActivity) }
        .doOnStop(customTab) { unbindCustomTabsService(this@AboutActivity) }
        .doOnDestroy(customTab) { connectionCallback = null }
        .autoDisposable(this)
        .subscribe()

    val viewGroup = viewContainer.forActivity(this)
    layoutInflater.inflate(R.layout.activity_generic_container, viewGroup)

    ButterKnife.bind(this).doOnDestroy { unbind() }
    router = Conductor.attachRouter(this, container, savedInstanceState)
    if (!router.hasRootController()) {
      router.setRoot(RouterTransaction.with(AboutController()))
    }
  }

  override fun onBackPressed() {
    if (!router.handleBack()) {
      super.onBackPressed()
    }
  }

  @dagger.Module
  abstract class Module {

    @Binds
    @PerActivity
    abstract fun provideActivity(activity: AboutActivity): Activity

  }
}

class AboutController : ButterKnifeController() {

  companion object {
    private const val PAGE_TAG = "AboutController.pageTag"
    private const val FADE_PERCENT = 0.75F
    private const val TITLE_TRANSLATION_PERCENT = 0.50F
  }

  @Inject
  internal lateinit var customTab: CustomTabActivityHelper
  @Inject
  internal lateinit var linkManager: LinkManager
  @Inject
  internal lateinit var bypass: Bypass

  @BindView(R.id.about_controller_root)
  lateinit var rootLayout: CoordinatorLayout
  @BindView(R.id.appbarlayout)
  lateinit var appBarLayout: AppBarLayout
  @BindView(R.id.banner_container)
  lateinit var bannerContainer: View
  @BindView(R.id.banner_icon)
  lateinit var bannerIcon: ImageView
  @BindView(R.id.banner_text)
  lateinit var aboutText: TextView
  @BindView(R.id.banner_title)
  lateinit var title: TextView
  @BindView(R.id.tab_layout)
  lateinit var tabLayout: TabLayout
  @BindView(R.id.toolbar)
  lateinit var toolbar: Toolbar
  @BindView(R.id.view_pager)
  lateinit var viewPager: ViewPager

  private var pagerAdapter: RouterPagerAdapter
  private val compositeClickSpan = { url: String ->
    setOf(
        object : TouchableUrlSpan(url, aboutText.linkTextColors, 0) {
          override fun onClick(url: String) {
            linkManager.openUrl(
                UrlMeta(url, aboutText.highlightColor,
                    activity!!))
                .subscribe()
          }
        },
        StyleSpan(Typeface.BOLD)
    )
  }

  init {
    pagerAdapter = object : RouterPagerAdapter(this) {
      override fun configureRouter(router: Router, position: Int) {
        if (!router.hasRootController()) {
          val controller: Controller = when (position) {
            0 -> LicensesController()
            1 -> ChangelogController()
            else -> TODO("Not implemented")
          }
          router.setRoot(RouterTransaction.with(controller)
              .tag(PAGE_TAG))
        }
      }

      override fun getCount() = 2

      override fun getPageTitle(position: Int) = when (position) {
        0 -> resources!!.getString(R.string.licenses)
        else -> resources!!.getString(R.string.changelog)
      }
    }
  }

  override fun onContextAvailable(context: Context) {
    ConductorInjection.inject(this)
    super.onContextAvailable(context)
  }

  override fun inflateView(inflater: LayoutInflater, container: ViewGroup): View =
      inflater.inflate(R.layout.controller_about, container, false)

  override fun bind(view: View) = ButterKnife.bind(this, view)

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

  @SuppressLint("SetTextI18n")
  override fun onViewBound(view: View) {
    super.onViewBound(view)
    with(activity as AppCompatActivity) {
      if (!isInNightMode()) {
        toolbar.setLightStatusBar()
      }
      setSupportActionBar(toolbar)
      supportActionBar?.run {
        setDisplayHomeAsUpEnabled(true)
        setDisplayShowTitleEnabled(false)
      }
    }

    bannerIcon.setOnClickListener {
      appBarLayout.setExpanded(false, true)
    }
    bannerIcon.setOnLongClickListener {
      Toast.makeText(activity, R.string.icon_attribution, Toast.LENGTH_SHORT).show()
      linkManager.openUrl(
          UrlMeta("https://cookicons.co", aboutText.highlightColor, activity!!))
          .subscribe()
      true
    }

    aboutText.movementMethod = LinkTouchMovementMethod.getInstance()
    aboutText.text = buildMarkdown {
      text(aboutText.resources.getString(R.string.about_description))
      newline(3)
      text(aboutText.resources.getString(R.string.about_version, BuildConfig.VERSION_NAME))
      newline(2)
      text(aboutText.resources.getString(R.string.about_by))
      space()
      link("https://twitter.com/pandanomic", "Zac Sweers")
      text(" - ")
      link("https://github.com/hzsweers/CatchUp",
          aboutText.resources.getString(R.string.about_source_code))
    }.parseMarkdownAndPlainLinks(
        on = aboutText,
        with = bypass,
        alternateSpans = compositeClickSpan)

    setUpPager()

    // Wait till things are measured
    val callSetUpHeader = { setUpHeader() }
    if (!appBarLayout.isLaidOut) {
      appBarLayout.post { callSetUpHeader() }
    } else {
      callSetUpHeader()
    }
  }

  private fun setUpPager() {
    viewPager.adapter = pagerAdapter
    tabLayout.setupWithViewPager(viewPager, false)

    tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
      override fun onTabSelected(tab: TabLayout.Tab) {}

      override fun onTabUnselected(tab: TabLayout.Tab) {}

      override fun onTabReselected(tab: TabLayout.Tab) {
        pagerAdapter.getRouter(tab.position)?.getControllerWithTag(PAGE_TAG)?.let {
          if (it is Scrollable) {
            it.onRequestScrollToTop()
          }
        }
      }
    })
  }

  private fun setUpHeader() {
    // TODO would be good if we could be smarter about scroll distance and tweak the offset
    // thresholds to dynamically adjust. Case and point - don't want tablayout below to cover the
    // text before it's fully faded out
    val parallaxMultiplier = (bannerContainer.layoutParams as CollapsingToolbarLayout.LayoutParams).parallaxMultiplier

    val interpolator = UiUtil.fastOutSlowInInterpolator

    // X is pretty simple, we just want to end up 72dp to the end of start
    @Px val finalAppBarHeight = toolbar.measuredHeight * 2
    @Px val translatableHeight = appBarLayout.measuredHeight - finalAppBarHeight
    @Px val titleX = title.x
    @Px val titleInset = toolbar.titleMarginStart
    @Px val desiredTitleX = if (Locale.getDefault().layoutDirection == View.LAYOUT_DIRECTION_RTL) {
      // I have to subtract 2x to line this up correctly
      // I have no idea why
      toolbar.measuredWidth - titleInset - titleInset
    } else {
      titleInset
    }
    @Px val xDelta = titleX - desiredTitleX

    // Y values are a bit trickier - these need to figure out where they would be on the larger
    // plane, so we calculate it upfront by predicting where it would land after collapse is done.
    // This requires knowing the parallax multiplier and adjusting for the parent plane rather
    // than the relative plane of the internal LL. Once we know the predicted global Y, easy to
    // calculate desired delta from there.
    @Px val titleY = title.y
    @Px val desiredTitleY = (toolbar.measuredHeight - title.measuredHeight) / 2
    @Px val predictedFinalY = titleY - (translatableHeight * parallaxMultiplier)
    @Px val yDelta = desiredTitleY - predictedFinalY

    /*
     * Here we want to get the appbar offset changes paired with the direction it's moving and
     * using RxBinding's great `offsetChanges` API to make an rx Observable of this. The first
     * part buffers two while skipping one at a time and emits "scroll direction" enums. Second
     * part is just a simple map to pair the offset with the resolved scroll direction comparing
     * to the previous offset. This gives us a nice stream of (offset, direction) emissions.
     *
     * Note that the filter() is important if you manipulate child views of the ABL. If any child
     * view requests layout again, it will trigger an emission from the offset listener with the
     * same value as before, potentially causing measure/layout/draw thrashing if your logic
     * reacting to the offset changes *is* manipulating those child views (vicious cycle).
     */
    RxAppBarLayout.offsetChanges(appBarLayout)
        .buffer(2, 1) // Buffer in pairs to compare the previous, skip none
        .filter { it[1] != it[0] }
        .map {
          // Map to a direction
          it[1] to ScrollDirection.resolve(it[1], it[0])
        }
        .subscribe { (offset, _) ->
          // Note: Direction is unused for now but left because this was neat
          val percentage = Math.abs(offset).toFloat() / translatableHeight

          // Force versions outside boundaries to be safe
          if (percentage > FADE_PERCENT) {
            bannerIcon.alpha = 0F
            aboutText.alpha = 0F
          }
          if (percentage < TITLE_TRANSLATION_PERCENT) {
            title.translationX = 0F
            title.translationY = 0F
          }
          if (percentage < FADE_PERCENT) {
            // We want to accelerate fading to be the first [FADE_PERCENT]% of the translation,
            // so adjust accordingly below and use the new calculated percentage for our
            // interpolation
            val adjustedPercentage = 1 - (percentage * (1.0F / FADE_PERCENT))
            val interpolation = interpolator.getInterpolation(adjustedPercentage)
            bannerIcon.alpha = interpolation
            aboutText.alpha = interpolation
          }
          if (percentage > TITLE_TRANSLATION_PERCENT) {
            // Start translating about halfway through (to give a staggered effect next to the alpha
            // so they have time to fade out sufficiently). From here we just set translation offsets
            // to adjust the position naturally to give the appearance of settling in to the right
            // place.
            val adjustedPercentage = (1 - percentage) * (1.0F / TITLE_TRANSLATION_PERCENT)
            val interpolation = interpolator.getInterpolation(adjustedPercentage)
            title.translationX = -(xDelta - (interpolation * xDelta))
            title.translationY = yDelta - (interpolation * yDelta)
          }
        }
  }
}

@PerController
@Subcomponent
interface AboutComponent : AndroidInjector<AboutController> {

  @Subcomponent.Builder
  abstract class Builder : AndroidInjector.Builder<AboutController>()
}

@Module(subcomponents = arrayOf(
    AboutComponent::class,
    LicensesComponent::class,
    ChangelogComponent::class
))
abstract class AboutControllerBindingModule {

  @Binds
  @IntoMap
  @ControllerKey(AboutController::class)
  internal abstract fun bindAboutControllerInjectorFactory(
      builder: AboutComponent.Builder): AndroidInjector.Factory<out Controller>

  @Binds
  @IntoMap
  @ControllerKey(LicensesController::class)
  internal abstract fun bindLicensesControllerInjectorFactory(
      builder: LicensesComponent.Builder): AndroidInjector.Factory<out Controller>

  @Binds
  @IntoMap
  @ControllerKey(ChangelogController::class)
  internal abstract fun bindChangelogControllerInjectorFactory(
      builder: ChangelogComponent.Builder): AndroidInjector.Factory<out Controller>
}

private enum class ScrollDirection {
  UP, DOWN;

  companion object {
    fun resolve(current: Int, prev: Int): ScrollDirection {
      return if (current > prev) {
        DOWN
      } else {
        UP
      }
    }
  }
}
