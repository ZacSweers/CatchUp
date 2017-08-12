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

import android.os.Bundle
import android.view.ViewGroup
import butterknife.BindView
import butterknife.ButterKnife
import com.bluelinelabs.conductor.Conductor
import com.bluelinelabs.conductor.Router
import com.bluelinelabs.conductor.RouterTransaction
import com.f2prateek.rx.preferences2.Preference
import dagger.Provides
import io.sweers.catchup.P
import io.sweers.catchup.R
import io.sweers.catchup.data.LinkManager
import io.sweers.catchup.injection.qualifiers.preferences.SmartLinking
import io.sweers.catchup.injection.scopes.PerActivity
import io.sweers.catchup.ui.ViewContainer
import io.sweers.catchup.ui.base.BaseActivity
import io.sweers.catchup.ui.controllers.PagerController
import io.sweers.catchup.util.customtabs.CustomTabActivityHelper
import javax.inject.Inject

class MainActivity : BaseActivity() {

  @Inject internal lateinit var customTab: CustomTabActivityHelper
  @Inject internal lateinit var viewContainer: ViewContainer
  @Inject internal lateinit var linkManager: LinkManager

  @BindView(R.id.controller_container) internal lateinit var container: ViewGroup

  private lateinit var router: Router

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    customTab.doOnDestroy { connectionCallback = null }
    val viewGroup = viewContainer.forActivity(this)
    layoutInflater.inflate(R.layout.activity_main, viewGroup)

    ButterKnife.bind(this).doOnDestroy { unbind() }

    router = Conductor.attachRouter(this, container, savedInstanceState)
    if (!router.hasRootController()) {
      router.setRoot(RouterTransaction.with(PagerController()))
    }
  }

  override fun onStart() {
    super.onStart()
    customTab.bindCustomTabsService(this)
    linkManager.connect(this)
  }

  override fun onStop() {
    customTab.unbindCustomTabsService(this)
    super.onStop()
  }

  override fun onBackPressed() {
    if (!router.handleBack()) {
      super.onBackPressed()
    }
  }

  @dagger.Module
  object Module {

    @Provides
    @JvmStatic
    @PerActivity
    internal fun provideCustomTabActivityHelper(): CustomTabActivityHelper {
      return CustomTabActivityHelper()
    }

    @Provides
    @SmartLinking
    @JvmStatic
    @PerActivity
    internal fun provideSmartLinkingPref(): Preference<Boolean> {
      return P.SmartlinkingGlobal.rx()
    }

    @Provides
    @JvmStatic
    @PerActivity
    internal fun provideLinkManager(helper: CustomTabActivityHelper,
        @SmartLinking linkingPref: Preference<Boolean>): LinkManager {
      return LinkManager(helper, linkingPref)
    }
  }
}
