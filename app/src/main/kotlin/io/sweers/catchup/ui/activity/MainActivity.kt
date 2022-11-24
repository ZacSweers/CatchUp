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
import android.content.SharedPreferences
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commitNow
import androidx.recyclerview.widget.RecyclerView.RecycledViewPool
import com.squareup.anvil.annotations.ContributesMultibinding
import com.squareup.anvil.annotations.ContributesTo
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.Multibinds
import dev.zacsweers.catchup.appconfig.AppConfig
import dev.zacsweers.catchup.di.AppScope
import dev.zacsweers.catchup.di.SingleIn
import dev.zacsweers.catchup.di.android.ActivityKey
import io.sweers.catchup.R
import io.sweers.catchup.base.ui.InjectingBaseActivity
import io.sweers.catchup.data.LinkManager
import io.sweers.catchup.data.ServiceDao
import io.sweers.catchup.databinding.ActivityMainBinding
import io.sweers.catchup.edu.Syllabus
import io.sweers.catchup.service.api.ScrollableContent
import io.sweers.catchup.service.api.Service
import io.sweers.catchup.service.api.ServiceMeta
import io.sweers.catchup.ui.DetailDisplayer
import io.sweers.catchup.ui.fragments.PagerFragment
import io.sweers.catchup.ui.fragments.service.StorageBackedService
import io.sweers.catchup.util.customtabs.CustomTabActivityHelper
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Qualifier
import me.saket.inboxrecyclerview.InboxRecyclerView
import me.saket.inboxrecyclerview.dimming.DimPainter
import me.saket.inboxrecyclerview.page.ExpandablePageLayout
import me.saket.inboxrecyclerview.page.InterceptResult
import me.saket.inboxrecyclerview.page.SimplePageStateChangeCallbacks

@ActivityKey(MainActivity::class)
@ContributesMultibinding(AppScope::class, boundType = Activity::class)
class MainActivity
@Inject
constructor(
  private val customTab: CustomTabActivityHelper,
  private val linkManager: LinkManager,
  private val syllabus: Syllabus,
  private val pagerFragmentProvider: Provider<PagerFragment>,
) : InjectingBaseActivity() {

  internal lateinit var detailPage: ExpandablePageLayout
  internal var pagerFragment: PagerFragment? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    syllabus.bind(this)

    val binding = viewContainer.inflateBinding(ActivityMainBinding::inflate)
    detailPage = binding.detailPage

    if (savedInstanceState == null) {
      supportFragmentManager.commitNow {
        add(R.id.fragment_container, pagerFragmentProvider.get().also { pagerFragment = it })
      }
    } else {
      pagerFragment =
        supportFragmentManager.findFragmentById(R.id.fragment_container) as PagerFragment
    }
  }

  override fun onStart() {
    super.onStart()
    linkManager.connect(this)
    customTab.bindCustomTabsService(this)
  }

  override fun onStop() {
    customTab.unbindCustomTabsService(this)
    super.onStop()
  }

  override fun onDestroy() {
    customTab.connectionCallback = null
    super.onDestroy()
  }

  @ContributesTo(AppScope::class)
  @Module
  abstract class ServiceIntegrationModule {
    companion object {
      // TODO de-scope
      @TextViewPool @Provides fun provideTextViewPool() = RecycledViewPool()

      @VisualViewPool @Provides fun provideVisualViewPool() = RecycledViewPool()

      @SingleIn(AppScope::class)
      @Provides
      @FinalServices
      fun provideFinalServices(
        serviceDao: ServiceDao,
        serviceMetas: @JvmSuppressWildcards Map<String, ServiceMeta>,
        sharedPreferences: SharedPreferences,
        services: @JvmSuppressWildcards Map<String, Provider<Service>>,
        appConfig: AppConfig
      ): @JvmSuppressWildcards Map<String, Provider<Service>> {
        return services
          .filter {
            serviceMetas.getValue(it.key).enabled &&
              sharedPreferences.getBoolean(serviceMetas.getValue(it.key).enabledPreferenceKey, true)
          }
          .mapValues { (_, value) ->
            Provider { StorageBackedService(serviceDao, value.get(), appConfig) }
          }
      }
    }

    @Multibinds abstract fun services(): @JvmSuppressWildcards Map<String, Service>

    @Multibinds abstract fun serviceMetas(): @JvmSuppressWildcards Map<String, ServiceMeta>

    @Multibinds
    abstract fun fragmentCreators(): @JvmSuppressWildcards Map<Class<out Fragment>, Fragment>

    @SingleIn(AppScope::class)
    @Binds
    abstract fun provideDetailDisplayer(
      mainActivityDetailDisplayer: MainActivityDetailDisplayer
    ): DetailDisplayer
  }
}

@Qualifier annotation class TextViewPool

@Qualifier annotation class VisualViewPool

@Qualifier annotation class FinalServices

/** A displayer that repeatedly shows new detail views in a new ExpandablePageLayout. */
// TODO do something else with this
class MainActivityDetailDisplayer @Inject constructor(private val mainActivity: MainActivity) :
  DetailDisplayer {

  private var collapser: (() -> Unit)? = null

  private inline val detailPage
    get() = mainActivity.detailPage

  override val isExpandedOrExpanding
    get() = detailPage.isExpandedOrExpanding

  override fun showDetail(body: (ExpandablePageLayout, FragmentManager) -> () -> Unit) {
    collapser?.invoke()
    collapser = null
    detailPage.addStateChangeCallbacks(
      object : SimplePageStateChangeCallbacks() {
        override fun onPageCollapsed() {
          detailPage.removeStateChangeCallbacks(this)
          collapser = null
        }
      }
    )
    collapser = body(detailPage, mainActivity.supportFragmentManager)
  }

  override fun bind(irv: InboxRecyclerView, useExistingFragment: Boolean) {
    val targetFragment =
      if (useExistingFragment) {
        mainActivity.supportFragmentManager.findFragmentById(detailPage.id)
      } else {
        null
      }
    bind(irv, targetFragment)
  }

  override fun bind(irv: InboxRecyclerView, targetFragment: Fragment?) {
    // TODO this is ugly, do better
    mainActivity.pagerFragment?.appBarLayout?.let { detailPage.pushParentToolbarOnExpand(it) }
    irv.expandablePage = detailPage
    irv.dimPainter =
      DimPainter.listAndPage(
        color = ContextCompat.getColor(irv.context, R.color.colorPrimary),
        alpha = 0.65F
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
