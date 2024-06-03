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
package catchup.app.data

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import androidx.annotation.RequiresApi
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.collection.ArrayMap
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toComposeRect
import androidx.compose.ui.unit.Density
import androidx.window.layout.WindowMetricsCalculator
import catchup.app.CatchUpPreferences
import catchup.app.service.LinkHandler
import catchup.app.ui.activity.MainActivity
import catchup.app.util.customtabs.CustomTabActivityHelper
import catchup.appconfig.AppConfig
import catchup.appconfig.isSdkAtLeast
import catchup.di.AppScope
import catchup.di.SingleIn
import catchup.flowbinding.intentReceivers
import catchup.util.isInNightMode
import catchup.util.kotlin.any
import catchup.util.kotlin.mergeWith
import com.squareup.anvil.annotations.ContributesBinding
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import okhttp3.HttpUrl
import timber.log.Timber

@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
class LinkManager
@Inject
constructor(
  private val customTab: CustomTabActivityHelper,
  private val catchUpPreferences: CatchUpPreferences,
  private val appConfig: AppConfig,
) : LinkHandler {

  // Naive cache that tracks if we've already resolved for activities that can handle a given host
  // TODO Eventually replace this with something that's mindful of per-service prefs
  private val dumbCache = ArrayMap<String, Boolean>()
  private var currentActivityScope: CoroutineScope? = null
  private var windowSizeClass: (() -> WindowSizeClass)? = null
  private var currentActivity: MainActivity? = null

  fun connect(activity: MainActivity) {
    currentActivity = activity
    val scope = MainScope()
    currentActivityScope = scope
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    windowSizeClass = {
      val density = Density(activity)
      val metrics = WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(activity)
      val size = with(density) { metrics.bounds.toComposeRect().size.toDpSize() }
      WindowSizeClass.calculateFromSize(size)
    }

    // Invalidate the cache when a new install/update happens or prefs changed
    val filter = IntentFilter()
    if (!appConfig.isSdkAtLeast(29)) {
      @Suppress("DEPRECATION") filter.addAction(Intent.ACTION_INSTALL_PACKAGE)
    }
    filter.addAction(Intent.ACTION_PACKAGE_CHANGED)
    scope.launch {
      activity.intentReceivers(filter).mergeWith(catchUpPreferences.smartlinkingGlobal).collect {
        dumbCache.clear()
      }
    }
  }

  fun disconnect() {
    currentActivityScope?.cancel()
    currentActivityScope = null
    windowSizeClass = null
    currentActivity = null
  }

  /**
   * Neat little helper method to check if a [android.content.pm.ResolveInfo.match] is for a
   * specific match or not. This is useful for checking if the ResolveInfo instance itself is a
   * browser or not.
   *
   * @param inputMatch the match int as provided by the ResolveInfo result
   * @return `true` if it's a specific Uri match, `false` if not.
   */
  private fun isSpecificUriMatch(inputMatch: Int): Boolean {
    var match = inputMatch
    match = match and IntentFilter.MATCH_CATEGORY_MASK
    return match >= IntentFilter.MATCH_CATEGORY_HOST && match <= IntentFilter.MATCH_CATEGORY_PATH
  }

  override fun shareUrl(url: HttpUrl, title: String?): Boolean {
    val context = currentActivity ?: return false
    val intent =
      Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, url.toString())
        title?.let { putExtra(Intent.EXTRA_TITLE, it) }
      }
    context.startActivity(Intent.createChooser(intent, "Share URL"))
    return true
  }

  override suspend fun openUrl(url: HttpUrl, accentColor: Color): Boolean {
    val context = currentActivity ?: return false
    // TODO this isn't great, should we make a StateFlow backed by this?
    val uri = Uri.parse(url.toString())
    if (!catchUpPreferences.smartlinkingGlobal.first()) {
      Timber.tag("LinkManager").d("Smartlinking disabled, skipping query")
      openCustomTab(context, uri, accentColor)
      return false
    }

    val intent = Intent(Intent.ACTION_VIEW, uri)
    return when {
      !dumbCache.containsKey(uri.host) -> {
        Timber.tag("LinkManager").d("Smartlinking enabled, querying")
        queryAndOpen(context, uri, intent.applyFlags(), accentColor)
      }
      dumbCache[url.host] == true -> {
        context.startActivity(intent.applyFlags())
        true
      }
      else -> {
        openCustomTab(context, uri, accentColor)
      }
    }
  }

  private suspend fun queryAndOpen(
    context: Context,
    uri: Uri,
    intent: Intent,
    accentColor: Color,
  ): Boolean {
    val matchedUri =
      if (appConfig.isSdkAtLeast(30)) {
        queryAndOpen30(context, intent)
      } else {
        queryAndOpenLegacy(context, intent)
      }

    return if (matchedUri) {
      dumbCache[uri.host] = true
      true
    } else {
      dumbCache[uri.host] = false
      openCustomTab(context, uri, accentColor)
    }
  }

  /**
   * On API 30+, we can't query activities to handle intents. Instead, we do an old-fashioned
   * try/catch.
   */
  @RequiresApi(30)
  private fun queryAndOpen30(context: Context, inputIntent: Intent): Boolean {
    val intent =
      Intent(inputIntent).apply { flags = flags or Intent.FLAG_ACTIVITY_REQUIRE_NON_BROWSER }
    return try {
      context.startActivity(intent)
      true
    } catch (e: ActivityNotFoundException) {
      false
    }
  }

  private suspend fun queryAndOpenLegacy(context: Context, intent: Intent): Boolean {
    val manager = context.packageManager
    val hasMatch =
      flow {
          manager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).forEach {
            emit(it)
          }
        }
        .flowOn(Dispatchers.IO)
        .any { resolveInfo -> isSpecificUriMatch(resolveInfo.match) }

    return if (hasMatch) {
      context.startActivity(intent)
      true
    } else {
      false
    }
  }

  private fun Intent.applyFlags(): Intent = apply {
    windowSizeClass?.invoke()?.let { sizeClass ->
      if (sizeClass.widthSizeClass != WindowWidthSizeClass.Compact) {
        flags =
          Intent.FLAG_ACTIVITY_NEW_TASK or
            Intent.FLAG_ACTIVITY_MULTIPLE_TASK or
            Intent.FLAG_ACTIVITY_NEW_DOCUMENT or
            Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT
      }
    }
  }

  private fun openCustomTab(context: Context, uri: Uri, accentColor: Color): Boolean {
    // TODO source this from compose+settings
    // TODO this actually doesn't seem to make a difference as the scheme behavior seems to be
    //  controlled via chrome://flags and ignore whatever apps or system define.
    val colorScheme =
      if (context.isInNightMode()) {
        CustomTabsIntent.COLOR_SCHEME_DARK
      } else {
        CustomTabsIntent.COLOR_SCHEME_LIGHT
      }
    return customTab.openCustomTab(
      context,
      customTab
        .newCustomTabIntentBuilder()
        .setColorScheme(colorScheme)
        .setDefaultColorSchemeParams(
          CustomTabColorSchemeParams.Builder().setToolbarColor(accentColor.toArgb()).build()
        )
        .build()
        .apply { intent.applyFlags() },
      uri,
    )
  }
}
