/*
 * Copyright 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.sweers.catchup.util.customtabs

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsServiceConnection
import androidx.browser.customtabs.CustomTabsSession
import io.sweers.catchup.injection.scopes.PerActivity
import io.sweers.catchup.util.maybeStartActivity
import javax.inject.Inject

/**
 * This is a helper class to manage the connection to the Custom Tabs Service and
 *
 *
 * Adapted from github.com/GoogleChrome/custom-tabs-client
 */
@PerActivity
class CustomTabActivityHelper @Inject constructor() {
  private var customTabsSession: CustomTabsSession? = null
  private var client: CustomTabsClient? = null
  private var connection: CustomTabsServiceConnection? = null

  var connectionCallback: ConnectionCallback? = null

  /**
   * Opens the URL on a Custom Tab if possible; otherwise falls back to opening it via
   * `Intent.ACTION_VIEW`
   *
   * @param customTabsIntent a CustomTabsIntent to be used if Custom Tabs is available
   * @param uri the Uri to be opened
   */
  fun openCustomTab(context: Context, customTabsIntent: CustomTabsIntent, uri: Uri) {
    val packageName = CustomTabsHelper.getPackageNameToUse(context)

    // if we cant find a package name, it means there's no browser that supports
    // Custom Tabs installed. So, we fallback to a view intent
    packageName?.let {
      customTabsIntent.intent.`package` = it
      customTabsIntent.launchUrl(context, uri)
    } ?: context.maybeStartActivity(Intent(Intent.ACTION_VIEW, uri))
  }

  val customTabIntent: CustomTabsIntent.Builder
    get() = CustomTabsIntent.Builder(session).setShowTitle(true)
        .enableUrlBarHiding()
        .addDefaultShareMenuItem()

  /**
   * Binds the Activity to the Custom Tabs Service
   *
   * @param activity the activity to be bound to the service
   */
  fun bindCustomTabsService(activity: Activity) {
    client?.run { return }

    val packageName = CustomTabsHelper.getPackageNameToUse(activity) ?: return
    connection = object : CustomTabsServiceConnection() {
      override fun onCustomTabsServiceConnected(name: ComponentName, client: CustomTabsClient) {
        this@CustomTabActivityHelper.client = client
        this@CustomTabActivityHelper.client?.warmup(0L)
        connectionCallback?.onCustomTabsConnected()
        //Initialize a session as soon as possible.
        session
      }

      override fun onServiceDisconnected(name: ComponentName) {
        client = null
        connectionCallback?.onCustomTabsDisconnected()
      }
    }
    CustomTabsClient.bindCustomTabsService(activity, packageName, connection)
  }

  /**
   * Unbinds the Activity from the Custom Tabs Service
   *
   * @param activity the activity that is bound to the service
   */
  fun unbindCustomTabsService(activity: Activity) {
    connection ?: return
    activity.unbindService(connection)
    client = null
    customTabsSession = null
  }

  /**
   * Creates or retrieves an exiting CustomTabsSession
   *
   * @return a CustomTabsSession
   */
  val session: CustomTabsSession?
    get() {
      return when {
        client == null -> null
        customTabsSession == null -> client!!.newSession(null)
        else -> customTabsSession
      }?.also {
        customTabsSession = it
      }
    }

  /**
   * @return true if call to mayLaunchUrl was accepted
   * @see {@link CustomTabsSession.mayLaunchUrl
   */
  fun mayLaunchUrl(uri: Uri, extras: Bundle, otherLikelyBundles: List<Bundle>): Boolean {
    client ?: return false
    val session = session ?: return false
    return session.mayLaunchUrl(uri, extras, otherLikelyBundles)
  }

  /**
   * A Callback for when the service is connected or disconnected. Use those callbacks to
   * handle UI changes when the service is connected or disconnected
   */
  interface ConnectionCallback {
    /**
     * Called when the service is connected
     */
    fun onCustomTabsConnected()

    /**
     * Called when the service is disconnected
     */
    fun onCustomTabsDisconnected()
  }
}
