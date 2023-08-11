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
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.foundation.NavigableCircuitContent
import com.slack.circuit.foundation.push
import com.slack.circuit.foundation.rememberCircuitNavigator
import com.slack.circuit.overlay.ContentWithOverlays
import com.slack.circuit.runtime.Screen
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
import io.sweers.catchup.ui.about.AboutScreen
import io.sweers.catchup.util.customtabs.CustomTabActivityHelper
import javax.inject.Inject
import timber.log.Timber

@ActivityKey(MainActivity::class)
@ContributesMultibinding(AppScope::class, boundType = Activity::class)
class MainActivity
@Inject
constructor(
  private val customTab: CustomTabActivityHelper,
  private val linkManager: LinkManager,
  private val circuit: Circuit,
  private val catchUpPreferences: CatchUpPreferences,
  private val rootContent: RootContent,
  appConfig: AppConfig,
) : AppCompatActivity() {

  init {
    if (appConfig.sdkInt == 29 && isTaskRoot) {
      onBackPressedDispatcher.addCallback {
        // https://twitter.com/Piwai/status/1169274622614704129
        // https://issuetracker.google.com/issues/139738913
        finishAfterTransition()
      }
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge(navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT))
    linkManager.connect(this)

    setContent {
      val dayNightAuto by catchUpPreferences.dayNightAuto.collectAsState(initial = true)
      val forceNight by catchUpPreferences.dayNightForceNight.collectAsState(initial = false)
      val useDynamicTheme by catchUpPreferences.dynamicTheme.collectAsState(initial = false)
      val useDarkTheme =
        if (dayNightAuto) {
          isSystemInDarkTheme()
        } else {
          forceNight
        }
      SideEffect {
        Timber.d(
          "Setting theme to $useDarkTheme. dayNightAuto: $dayNightAuto, forceNight: $forceNight, dynamic: $useDynamicTheme"
        )
      }
      CatchUpTheme(useDarkTheme = useDarkTheme, isDynamicColor = useDynamicTheme) {
        CircuitCompositionLocals(circuit) {
          ContentWithOverlays {
            val backstack = rememberSaveableBackStack {
              push(HomeScreen)
              intent?.parseRoute()?.forEach(::push)
            }
            val navigator = rememberCircuitNavigator(backstack)
            val intentAwareNavigator = remember(navigator) { IntentAwareNavigator(this, navigator) }
            rootContent.Content(intentAwareNavigator) {
              NavigableCircuitContent(
                intentAwareNavigator,
                backstack,
                // decoration = ImageViewerAwareNavDecoration()
              )
            }
          }
        }
      }
    }
  }

  override fun onStart() {
    super.onStart()
    customTab.bindCustomTabsService(this)
  }

  override fun onStop() {
    customTab.unbindCustomTabsService(this)
    super.onStop()
  }

  override fun onDestroy() {
    linkManager.disconnect()
    customTab.connectionCallback = null
    super.onDestroy()
  }

  companion object {
    private fun routeFor(segment: String, queryParams: Map<String, String?>): Screen {
      return when (segment) {
        "settings" -> SettingsScreen
        "about" -> AboutScreen(AboutScreen.AboutScreenComponent.componentFor(queryParams["tab"]))
        else -> throw IllegalArgumentException("Unknown path segment $segment")
      }
    }

    private fun Intent.parseRoute(): List<Screen> {
      // -a android.intent.action.VIEW -d "catchup://home/settings/about/?tab=changelog"
      // io.sweers.catchup
      return if (action == Intent.ACTION_VIEW) {
        data
          ?.let { uri ->
            val queryParams = uri.queryParameterNames.associateWith { uri.getQueryParameter(it) }
            uri.pathSegments.mapNotNull { segment -> routeFor(segment, queryParams) }
          }
          .orEmpty()
      } else {
        emptyList()
      }
    }
  }

  @ContributesTo(AppScope::class)
  @Module
  abstract class ServiceIntegrationModule {
    @Multibinds abstract fun services(): @JvmSuppressWildcards Map<String, Service>

    @Multibinds abstract fun serviceMetas(): @JvmSuppressWildcards Map<String, ServiceMeta>
  }
}
