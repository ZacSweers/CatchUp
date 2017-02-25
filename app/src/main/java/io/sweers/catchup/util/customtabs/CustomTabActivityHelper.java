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

package io.sweers.catchup.util.customtabs;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.customtabs.CustomTabsClient;
import android.support.customtabs.CustomTabsIntent;
import android.support.customtabs.CustomTabsServiceConnection;
import android.support.customtabs.CustomTabsSession;
import java.util.List;

/**
 * This is a helper class to manage the connection to the Custom Tabs Service and
 * <p>
 * Adapted from github.com/GoogleChrome/custom-tabs-client
 */
public class CustomTabActivityHelper {
  private CustomTabsSession customTabsSession;
  private CustomTabsClient client;
  private CustomTabsServiceConnection connection;
  private ConnectionCallback connectionCallback;

  /**
   * Opens the URL on a Custom Tab if possible; otherwise falls back to opening it via
   * {@code Intent.ACTION_VIEW}
   *
   * @param customTabsIntent a CustomTabsIntent to be used if Custom Tabs is available
   * @param uri the Uri to be opened
   */
  public void openCustomTab(Context context, CustomTabsIntent customTabsIntent, Uri uri) {
    String packageName = CustomTabsHelper.getPackageNameToUse(context);

    // if we cant find a package name, it means there's no browser that supports
    // Custom Tabs installed. So, we fallback to a view intent
    if (packageName != null) {
      customTabsIntent.intent.setPackage(packageName);
      customTabsIntent.launchUrl(context, uri);
    } else {
      context.startActivity(new Intent(Intent.ACTION_VIEW, uri));
    }
  }

  public CustomTabsIntent.Builder getCustomTabIntent() {
    return new CustomTabsIntent.Builder(getSession()).setShowTitle(true)
        .enableUrlBarHiding()
        .addDefaultShareMenuItem();
  }

  /**
   * Binds the Activity to the Custom Tabs Service
   *
   * @param activity the activity to be bound to the service
   */
  public void bindCustomTabsService(Activity activity) {
    if (client != null) return;

    String packageName = CustomTabsHelper.getPackageNameToUse(activity);
    if (packageName == null) return;
    connection = new CustomTabsServiceConnection() {
      @Override
      public void onCustomTabsServiceConnected(ComponentName name, CustomTabsClient client) {
        CustomTabActivityHelper.this.client = client;
        CustomTabActivityHelper.this.client.warmup(0L);
        if (connectionCallback != null) connectionCallback.onCustomTabsConnected();
        //Initialize a session as soon as possible.
        getSession();
      }

      @Override public void onServiceDisconnected(ComponentName name) {
        client = null;
        if (connectionCallback != null) connectionCallback.onCustomTabsDisconnected();
      }
    };
    CustomTabsClient.bindCustomTabsService(activity, packageName, connection);
  }

  /**
   * Unbinds the Activity from the Custom Tabs Service
   *
   * @param activity the activity that is bound to the service
   */
  public void unbindCustomTabsService(Activity activity) {
    if (connection == null) return;
    activity.unbindService(connection);
    client = null;
    customTabsSession = null;
  }

  /**
   * Creates or retrieves an exiting CustomTabsSession
   *
   * @return a CustomTabsSession
   */
  public CustomTabsSession getSession() {
    if (client == null) {
      customTabsSession = null;
    } else if (customTabsSession == null) {
      customTabsSession = client.newSession(null);
    }
    return customTabsSession;
  }

  /**
   * Register a Callback to be called when connected or disconnected from the Custom Tabs Service
   */
  public void setConnectionCallback(ConnectionCallback connectionCallback) {
    this.connectionCallback = connectionCallback;
  }

  /**
   * @return true if call to mayLaunchUrl was accepted
   * @see {@link CustomTabsSession#mayLaunchUrl(Uri, Bundle, List)}
   */
  public boolean mayLaunchUrl(Uri uri, Bundle extras, List<Bundle> otherLikelyBundles) {
    if (client == null) return false;

    CustomTabsSession session = getSession();
    if (session == null) return false;

    return session.mayLaunchUrl(uri, extras, otherLikelyBundles);
  }

  /**
   * A Callback for when the service is connected or disconnected. Use those callbacks to
   * handle UI changes when the service is connected or disconnected
   */
  public interface ConnectionCallback {
    /**
     * Called when the service is connected
     */
    void onCustomTabsConnected();

    /**
     * Called when the service is disconnected
     */
    void onCustomTabsDisconnected();
  }
}
