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
import butterknife.Unbinder
import com.bluelinelabs.conductor.Conductor
import com.bluelinelabs.conductor.Router
import com.bluelinelabs.conductor.RouterTransaction
import io.sweers.catchup.R
import io.sweers.catchup.data.LinkManager
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
  private var unbinder: Unbinder? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val viewGroup = viewContainer.forActivity(this)
    layoutInflater.inflate(R.layout.activity_main, viewGroup)

    unbinder = ButterKnife.bind(this)

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

  override fun onDestroy() {
    customTab.connectionCallback = null
    unbinder?.unbind()
    super.onDestroy()
  }
}
