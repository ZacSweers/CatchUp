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
package io.sweers.catchup.ui.about

import android.app.Activity
import android.os.Bundle
import androidx.compose.ui.platform.ComposeView
import autodispose2.autoDispose
import com.slack.circuit.CircuitCompositionLocals
import com.slack.circuit.CircuitConfig
import com.slack.circuit.CircuitContent
import com.squareup.anvil.annotations.ContributesMultibinding
import dev.zacsweers.catchup.compose.CatchUpTheme
import dev.zacsweers.catchup.di.AppScope
import dev.zacsweers.catchup.di.android.ActivityKey
import io.sweers.catchup.base.ui.InjectingBaseActivity
import io.sweers.catchup.util.customtabs.CustomTabActivityHelper
import javax.inject.Inject

@ActivityKey(AboutActivity::class)
@ContributesMultibinding(AppScope::class, boundType = Activity::class)
class AboutActivity
@Inject
constructor(
  private val customTab: CustomTabActivityHelper,
  private val circuitConfig: CircuitConfig,
) : InjectingBaseActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    lifecycle()
      .doOnStart(customTab) { bindCustomTabsService(this@AboutActivity) }
      .doOnStop(customTab) { unbindCustomTabsService(this@AboutActivity) }
      .doOnDestroy(customTab) { connectionCallback = null }
      .autoDispose(this)
      .subscribe()

    val viewGroup = viewContainer.forActivity(this)
    val composeView = ComposeView(this)
    viewGroup.addView(composeView)
    composeView.setContent {
      CatchUpTheme { CircuitCompositionLocals(circuitConfig) { CircuitContent(AboutScreen) } }
    }
  }
}
