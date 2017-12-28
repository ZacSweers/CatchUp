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

package io.sweers.catchup.ui.activity

import android.app.Activity
import android.os.Bundle
import android.support.v7.widget.RecyclerView.RecycledViewPool
import android.view.ViewGroup
import butterknife.BindView
import butterknife.ButterKnife
import com.bluelinelabs.conductor.Conductor
import com.bluelinelabs.conductor.Router
import com.bluelinelabs.conductor.RouterTransaction
import com.uber.autodispose.kotlin.autoDisposable
import dagger.Binds
import dagger.Provides
import dagger.multibindings.Multibinds
import io.sweers.catchup.R
import io.sweers.catchup.data.LinkManager
import io.sweers.catchup.data.ServiceDao
import io.sweers.catchup.edu.Syllabus
import io.sweers.catchup.injection.scopes.PerActivity
import io.sweers.catchup.service.api.LinkHandler
import io.sweers.catchup.service.api.Service
import io.sweers.catchup.service.api.ServiceMeta
import io.sweers.catchup.service.designernews.DesignerNewsModule
import io.sweers.catchup.service.dribbble.DribbbleModule
import io.sweers.catchup.service.github.GitHubModule
import io.sweers.catchup.service.hackernews.HackerNewsModule
import io.sweers.catchup.service.medium.MediumModule
import io.sweers.catchup.service.producthunt.ProductHuntModule
import io.sweers.catchup.service.reddit.RedditModule
import io.sweers.catchup.service.slashdot.SlashdotModule
import io.sweers.catchup.ui.base.BaseActivity
import io.sweers.catchup.ui.controllers.PagerController
import io.sweers.catchup.ui.controllers.service.StorageBackedService
import io.sweers.catchup.util.customtabs.CustomTabActivityHelper
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Qualifier

class MainActivity : BaseActivity() {

  @Inject internal lateinit var customTab: CustomTabActivityHelper
  @Inject internal lateinit var linkManager: LinkManager
  @Inject internal lateinit var syllabus: Syllabus

  @BindView(R.id.controller_container) internal lateinit var container: ViewGroup

  private lateinit var router: Router

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

    ButterKnife.bind(this).doOnDestroy { unbind() }

    router = Conductor.attachRouter(this, container, savedInstanceState)
    if (!router.hasRootController()) {
      router.setRoot(RouterTransaction.with(PagerController()))
    }
  }


  override fun onBackPressed() {
    if (!router.handleBack()) {
      super.onBackPressed()
    }
  }

  @dagger.Module(
      includes = [
      HackerNewsModule::class,
      RedditModule::class,
      MediumModule::class,
      ProductHuntModule::class,
      SlashdotModule::class,
      DesignerNewsModule::class,
      DribbbleModule::class,
      GitHubModule::class
//      ImgurModule::class
      ]
  )
  abstract class ServiceIntegrationModule {
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
      fun provideFinalServices(serviceDao: ServiceDao,
          services: Map<String, @JvmSuppressWildcards Provider<Service>>): Map<String, Provider<Service>> {
        return services.mapValues { (_, value) ->
          Provider<Service> {
            StorageBackedService(serviceDao, value.get())
          }
        }
      }
    }

    @Multibinds
    @PerActivity
    abstract fun services(): Map<String, Service>

    @Multibinds
    abstract fun serviceMetas(): Map<String, ServiceMeta>

    @Binds
    @PerActivity
    abstract fun provideActivity(activity: MainActivity): Activity

    @Binds
    @PerActivity
    abstract fun provideLinkHandler(linkManager: LinkManager): LinkHandler

  }
}

@Qualifier
annotation class TextViewPool

@Qualifier
annotation class VisualViewPool

@Qualifier
annotation class FinalServices
