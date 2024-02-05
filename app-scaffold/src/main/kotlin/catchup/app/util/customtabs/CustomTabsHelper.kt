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
package catchup.app.util.customtabs

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.browser.customtabs.CustomTabsService
import androidx.core.net.toUri
import timber.log.Timber

/**
 * Helper class for Custom Tabs.
 *
 * Adapted from github.com/GoogleChrome/custom-tabs-client
 */
object CustomTabsHelper {
  private const val STABLE_PACKAGE = "com.android.chrome"
  private const val BETA_PACKAGE = "com.chrome.beta"
  private const val DEV_PACKAGE = "com.chrome.dev"
  private const val LOCAL_PACKAGE = "com.google.android.apps.chrome"
  private const val TAG = "CustomTabsHelper"

  private var sPackageNameToUse: String? = null

  /**
   * Goes through all apps that handle VIEW intents and have a warmup service. Picks the one chosen
   * by the user if there is one, otherwise makes a best effort to return a valid package name.
   *
   * This is **not** threadsafe.
   *
   * @param context [Context] to use for accessing [PackageManager].
   * @return The package name recommended to use for connecting to custom tabs related components.
   */
  @Suppress("DEPRECATION")
  fun getPackageNameToUse(context: Context): String? {
    sPackageNameToUse?.run {
      return this
    }

    val pm = context.packageManager
    // Get default VIEW intent handler.
    val activityIntent = Intent(Intent.ACTION_VIEW, "http://www.example.com".toUri())
    val defaultViewHandlerInfo = pm.resolveActivity(activityIntent, 0)
    var defaultViewHandlerPackageName: String? = null
    if (defaultViewHandlerInfo != null) {
      defaultViewHandlerPackageName = defaultViewHandlerInfo.activityInfo.packageName
    }

    // Get all apps that can handle VIEW intents.
    val resolvedActivityList = pm.queryIntentActivities(activityIntent, 0)
    val packagesSupportingCustomTabs = ArrayList<String>()
    for (info in resolvedActivityList) {
      val serviceIntent = Intent()
      serviceIntent.action = CustomTabsService.ACTION_CUSTOM_TABS_CONNECTION
      serviceIntent.`package` = info.activityInfo.packageName
      if (pm.resolveService(serviceIntent, 0) != null) {
        packagesSupportingCustomTabs.add(info.activityInfo.packageName)
      }
    }

    // Now packagesSupportingCustomTabs contains all apps that can handle both VIEW intents
    // and service calls.
    return when {
      packagesSupportingCustomTabs.isEmpty() -> null
      packagesSupportingCustomTabs.size == 1 -> packagesSupportingCustomTabs[0]
      !defaultViewHandlerPackageName.isNullOrBlank() &&
        !hasSpecializedHandlerIntents(context, activityIntent) &&
        packagesSupportingCustomTabs.contains(defaultViewHandlerPackageName) ->
        defaultViewHandlerPackageName
      STABLE_PACKAGE in packagesSupportingCustomTabs -> STABLE_PACKAGE
      BETA_PACKAGE in packagesSupportingCustomTabs -> BETA_PACKAGE
      DEV_PACKAGE in packagesSupportingCustomTabs -> DEV_PACKAGE
      LOCAL_PACKAGE in packagesSupportingCustomTabs -> LOCAL_PACKAGE
      else -> null
    }.apply { sPackageNameToUse = this }
  }

  /**
   * Used to check whether there is a specialized handler for a given intent.
   *
   * @param intent The intent to check with. *
   * @return Whether there is a specialized handler for the given intent.
   */
  @Suppress("DEPRECATION")
  private fun hasSpecializedHandlerIntents(context: Context, intent: Intent): Boolean {
    try {
      val pm = context.packageManager
      val handlers = pm.queryIntentActivities(intent, PackageManager.GET_RESOLVED_FILTER)
      if (handlers.size == 0) {
        return false
      }
      for (resolveInfo in handlers) {
        val filter = resolveInfo.filter ?: continue
        if (filter.countDataAuthorities() == 0 || filter.countDataPaths() == 0) continue
        if (resolveInfo.activityInfo == null) continue
        return true
      }
    } catch (e: RuntimeException) {
      Timber.tag(TAG).e("Runtime exception while getting specialized handlers")
    }

    return false
  }

  /** @return All possible chrome package names that provide custom tabs feature. */
  val packages: Array<String>
    get() = arrayOf("", STABLE_PACKAGE, BETA_PACKAGE, DEV_PACKAGE, LOCAL_PACKAGE)
}
