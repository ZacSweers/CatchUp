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
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.view.WindowCompat
import com.slack.circuit.CircuitCompositionLocals
import com.slack.circuit.CircuitConfig
import com.slack.circuit.NavigableCircuitContent
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.overlay.ContentWithOverlays
import com.slack.circuit.push
import com.slack.circuit.rememberCircuitNavigator
import com.squareup.anvil.annotations.ContributesMultibinding
import com.squareup.anvil.annotations.ContributesTo
import dagger.Module
import dagger.multibindings.Multibinds
import dev.zacsweers.catchup.appconfig.AppConfig
import dev.zacsweers.catchup.circuit.IntentAwareNavigator
import dev.zacsweers.catchup.compose.CatchUpTheme
import dev.zacsweers.catchup.di.AppScope
import dev.zacsweers.catchup.di.android.ActivityKey
import io.sweers.catchup.CatchUpPreferences
import io.sweers.catchup.base.ui.RootContent
import io.sweers.catchup.data.LinkManager
import io.sweers.catchup.home.HomeScreen
import io.sweers.catchup.service.api.Service
import io.sweers.catchup.service.api.ServiceMeta
import io.sweers.catchup.util.customtabs.CustomTabActivityHelper
import javax.inject.Inject

@ActivityKey(MainActivity::class)
@ContributesMultibinding(AppScope::class, boundType = Activity::class)
class MainActivity
@Inject
constructor(
  private val customTab: CustomTabActivityHelper,
  private val linkManager: LinkManager,
  private val circuitConfig: CircuitConfig,
  private val catchUpPreferences: CatchUpPreferences,
  private val rootContent: RootContent,
  private val appConfig: AppConfig,
) : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    WindowCompat.setDecorFitsSystemWindows(window, false)

    setContent {
      val dayNightAuto by catchUpPreferences.dayNightAuto.collectAsState(initial = true)
      val forceNight by catchUpPreferences.dayNightForceNight.collectAsState(initial = false)
      val useDarkTheme =
        if (dayNightAuto) {
          isSystemInDarkTheme()
        } else {
          forceNight
        }
      CatchUpTheme(useDarkTheme = useDarkTheme) {
        CircuitCompositionLocals(circuitConfig) {
          ContentWithOverlays {
            val backstack = rememberSaveableBackStack { push(HomeScreen) }
            val navigator = rememberCircuitNavigator(backstack)
            val intentAwareNavigator = remember(navigator) { IntentAwareNavigator(this, navigator) }
            rootContent.Content(intentAwareNavigator) {
              NavigableCircuitContent(
                intentAwareNavigator,
                backstack,
                enableBackHandler = true,
                decoration = ImageViewerAwareNavDecoration()
              )
            }
          }
        }
      }
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

  @Deprecated("Deprecated in Java")
  override fun onBackPressed() {
    if (appConfig.sdkInt == 29 && isTaskRoot) {
      // https://twitter.com/Piwai/status/1169274622614704129
      // https://issuetracker.google.com/issues/139738913
      finishAfterTransition()
    } else {
      super.onBackPressed()
    }
  }

  @ContributesTo(AppScope::class)
  @Module
  abstract class ServiceIntegrationModule {
    @Multibinds abstract fun services(): @JvmSuppressWildcards Map<String, Service>

    @Multibinds abstract fun serviceMetas(): @JvmSuppressWildcards Map<String, ServiceMeta>
  }
}
