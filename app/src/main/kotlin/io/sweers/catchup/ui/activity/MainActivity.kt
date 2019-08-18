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

import android.content.SharedPreferences
import android.os.Bundle
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
import io.sweers.catchup.service.api.Service
import io.sweers.catchup.service.api.ServiceMeta
import io.sweers.catchup.serviceregistry.ResolvedCatchUpServiceRegistry
import io.sweers.catchup.ui.DetailDisplayer
import io.sweers.catchup.ui.base.InjectingBaseActivity
import io.sweers.catchup.ui.fragments.PagerFragment
import io.sweers.catchup.ui.fragments.service.StorageBackedService
import io.sweers.catchup.util.customtabs.CustomTabActivityHelper
import kotterknife.bindView
import me.saket.inboxrecyclerview.InboxRecyclerView
import me.saket.inboxrecyclerview.page.ExpandablePageLayout
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

  override fun setFragmentFactory() {
    supportFragmentManager.fragmentFactory = fragmentFactory
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
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
        add(R.id.fragment_container, PagerFragment())
      }
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

    // TODO this is ugly, do better
    detailPage.pushParentToolbarOnExpand((mainActivity.supportFragmentManager.findFragmentById(R.id.fragment_container) as PagerFragment).toolbar)
    detailPage.addStateChangeCallbacks(object : SimplePageStateChangeCallbacks() {
      override fun onPageCollapsed() {
        detailPage.removeStateChangeCallbacks(this)
        collapser = null
      }
    })
    collapser = body(detailPage, mainActivity.supportFragmentManager)
  }

  override fun bindOnly(irv: InboxRecyclerView) {
    irv.expandablePage = detailPage
  }
}
