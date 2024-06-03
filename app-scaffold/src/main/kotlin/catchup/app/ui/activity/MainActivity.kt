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
package catchup.app.ui.activity

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import catchup.app.CatchUpPreferences
import catchup.app.data.LinkManager
import catchup.app.home.HomeScreen
import catchup.app.service.openUrl
import catchup.app.util.customtabs.CustomTabActivityHelper
import catchup.appconfig.AppConfig
import catchup.base.ui.RootContent
import catchup.base.ui.rememberSystemBarColorController
import catchup.compose.CatchUpTheme
import catchup.compose.LocalDisplayFeatures
import catchup.deeplink.DeepLinkHandler
import catchup.deeplink.parse
import catchup.di.AppScope
import catchup.di.android.ActivityKey
import catchup.service.api.Service
import catchup.service.api.ServiceMeta
import catchup.util.toDayContext
import catchup.util.toNightContext
import com.google.accompanist.adaptive.calculateDisplayFeatures
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.foundation.NavigableCircuitContent
import com.slack.circuit.foundation.rememberCircuitNavigator
import com.slack.circuit.overlay.ContentWithOverlays
import com.slack.circuitx.android.rememberAndroidScreenAwareNavigator
import com.slack.circuitx.gesturenavigation.GestureNavigationDecoration
import com.squareup.anvil.annotations.ContributesMultibinding
import com.squareup.anvil.annotations.ContributesTo
import dagger.Module
import dagger.multibindings.Multibinds
import javax.inject.Inject
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.runBlocking
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
  private val deepLinkHandler: DeepLinkHandler,
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
    installSplashScreen()
    enableEdgeToEdge(navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT))
    super.onCreate(savedInstanceState)
    linkManager.connect(this)

    setContent {
      val dayNightAuto by catchUpPreferences.dayNightAuto.collectAsState()
      val forceNight by catchUpPreferences.dayNightForceNight.collectAsState()
      val useDynamicTheme by catchUpPreferences.dynamicTheme.collectAsState()
      val useDarkTheme =
        if (dayNightAuto) {
          isSystemInDarkTheme()
        } else {
          forceNight
        }

      // Update the system UI content colors anytime our dark theme changes
      var systemUiSet by remember(useDarkTheme) { mutableStateOf(false) }
      if (!systemUiSet) {
        val systemUiController = rememberSystemBarColorController()
        systemUiController.systemBarsDarkContentEnabled = !useDarkTheme
        systemUiSet = true
      }

      val context = LocalContext.current
      val contextToUse =
        remember(context, dayNightAuto, forceNight, useDarkTheme) {
          // If we're not respecting dayNight or we're forcing night, use the dark theme
          if (!dayNightAuto && !useDarkTheme) {
            context.toDayContext()
          } else if (useDarkTheme) {
            context.toNightContext()
          } else {
            context
          }
        }
      SideEffect {
        Timber.d(
          "Setting theme to $useDarkTheme. dayNightAuto: $dayNightAuto, forceNight: $forceNight, dynamic: $useDynamicTheme"
        )
      }

      val uriHandler =
        object : UriHandler {
          override fun openUri(uri: String) {
            runBlocking { linkManager.openUrl(uri) }
          }
        }

      val displayFeatures = calculateDisplayFeatures(this)
      CompositionLocalProvider(
        LocalDisplayFeatures provides displayFeatures,
        // Override LocalContext to one that's set to our daynight modes, as many compose APIs use
        // LocalContext under the hood
        LocalContext provides contextToUse,
        LocalUriHandler provides uriHandler,
      ) {
        CatchUpTheme(useDarkTheme = useDarkTheme, isDynamicColor = useDynamicTheme) {
          CircuitCompositionLocals(circuit) {
            ContentWithOverlays {
              val stack = remember {
                intent?.let(deepLinkHandler::parse) ?: persistentListOf(HomeScreen)
              }
              val backStack = rememberSaveableBackStack(stack)
              val navigator = rememberCircuitNavigator(backStack)
              val intentAwareNavigator =
                rememberAndroidScreenAwareNavigator(navigator, this@MainActivity)
              rootContent.Content(intentAwareNavigator) {
                NavigableCircuitContent(
                  intentAwareNavigator,
                  backStack,
                  decoration =
                    GestureNavigationDecoration(
                      circuit.defaultNavDecoration,
                      // Pop the back stack once the user has gone 'back'
                      navigator::pop,
                    ),
                )
              }
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

  @ContributesTo(AppScope::class)
  @Module
  abstract class ServiceIntegrationModule {
    @Multibinds abstract fun services(): @JvmSuppressWildcards Map<String, Service>

    @Multibinds abstract fun serviceMetas(): @JvmSuppressWildcards Map<String, ServiceMeta>
  }
}
